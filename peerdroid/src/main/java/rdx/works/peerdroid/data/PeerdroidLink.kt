package rdx.works.peerdroid.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.completeWhenDisconnected
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.AnswerPayload.Companion.toAnswerPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import timber.log.Timber

interface PeerdroidLink {

    /**
     * Call this function to add a connection in the wallet settings.
     *
     */
    suspend fun addConnection(encryptionKey: ByteArray): Result<Unit>
}

internal class PeerdroidLinkImpl(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidLink {

    private val webSocketClient = WebSocketClient(applicationContext)
    private val webRtcManager = WebRtcManager(applicationContext)

    private var webSocketClientJob: Job? = null
    private var webRtcManagerJob: Job? = null

    // This CompletableDeferred will return a result when a peer connection
    // has been first connected and then disconnected.
    private lateinit var addConnectionDeferred: CompletableDeferred<Result<Unit>>

    override suspend fun addConnection(encryptionKey: ByteArray): Result<Unit> {
        addConnectionDeferred = CompletableDeferred()
        // get connection id from encryption key
        val connectionId = encryptionKey.blake2Hash().toHexString()
        Timber.d("\uD83D\uDDFC️ add new link connection with connectionId: $connectionId")

        withContext(ioDispatcher) {
            // Leave this method here because WebRTC takes too long to initialize its components
            // and to create the peer connection and initialize the data channel.
            // So by the time the web socket is open and listening the peer connection will be ready to negotiate.
            observePeerConnectionUntilEstablished()
            // and now establish the web socket
            webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
                .onSuccess {
                    listenForIncomingMessagesFromSignalingServer(webSocketClient)
                }
                .onFailure {
                    terminateWithError()
                }
        }

        return addConnectionDeferred.await()
    }

    @Suppress("LongMethod")
    private fun listenForIncomingMessagesFromSignalingServer(webSocketClient: WebSocketClient) {
        webSocketClientJob = webSocketClient
            .listenForMessages()
            .onStart { // for debugging
                Timber.d("\uD83D\uDDFC start observing incoming messages from signaling server ▶️️")
            }
            .onCompletion {
                Timber.d("\uD83D\uDDFC️️ end observing incoming messages from signaling server ⏹️")
            }
            .onEach { incomingMessage ->
                when (incomingMessage) {
                    is SignalingServerMessage.RemoteInfo.ClientConnected -> {
                        Timber.d(
                            "🗼️ \uD83D\uDCE1️ connector extension is connected with id: ${incomingMessage.remoteClientId} ⬇️ \uD83D\uDFE9"
                        )
                    }
                    is SignalingServerMessage.RemoteData.Offer -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ offer received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                        setRemoteDescriptionFromOffer(incomingMessage)
                        createAndSendAnswerToRemoteClient()
                    }
                    is SignalingServerMessage.RemoteData.Answer -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ answer received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                    }
                    is SignalingServerMessage.RemoteData.IceCandidate -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ ice candidate received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                        addRemoteIceCandidateInWebRtc(incomingMessage)
                    }
                    is SignalingServerMessage.Confirmation -> {
//                        Timber.d("🗼️ \uD83D\uDCE1️️ confirmation received for requestId: ${incomingMessage.requestId} ⬇️")
                    }
                    is SignalingServerMessage.Error.InvalidMessage -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ invalid message error: ${incomingMessage.errorMessage} ⬇️")
                    }
                    is SignalingServerMessage.RemoteInfo.MissingClient -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ missing connector extension error, request id: ${incomingMessage.requestId} ⬇️")
                    }
                    is SignalingServerMessage.RemoteInfo.ClientDisconnected -> {
                        Timber.d(
                            "🗼️ \uD83D\uDCE1️️ connector extension disconnected with id: ${incomingMessage.remoteClientId} ⬇️ \uD83D\uDFE5"
                        )
                    }
                    is SignalingServerMessage.Error.Validation -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ validation error ❗ ⬇️")
                        terminateWithError()
                    }
                    is SignalingServerMessage.Error.Unknown -> {
                        Timber.d("🗼️ \uD83D\uDCE1️️ unknown error ❗ ⬇️")
                        terminateWithError()
                    }
                }
            }
            .catch { exception ->
                Timber.e("🗼️ ⬇️ an exception occurred: ${exception.localizedMessage}")
                terminateWithError()
            }
            .flowOn(ioDispatcher)
            .cancellable()
            .launchIn(applicationScope)
    }

    // a peer connection executed its lifecycle:
    // created -> connecting -> connected -> disconnected
    private fun observePeerConnectionUntilEstablished() {
        webRtcManagerJob = webRtcManager
            .createPeerConnection("")
            .onStart { // for debugging
                Timber.d("🗼 ⚡ start observing webrtc events ▶️")
            }
            .onCompletion { // for debugging
                Timber.d("🗼 ⚡ end observing webrtc events ⏹️")
            }
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.d("🗼 ⚡ renegotiation needed 🆗")
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("🗼 ⚡ ice gathering state changed: ${event.state}")
                    }
                    is PeerConnectionEvent.IceCandidate -> {
                        Timber.d("🗼 ⚡ ice candidate generated")
                        sendIceCandidateToRemoteClient(event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("🗼 ⚡ signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Connected -> {
                        Timber.d("🗼 ⚡ signaling state changed: peer connection connected 🟢")
                    }
                    is PeerConnectionEvent.Disconnected -> {
                        Timber.d("🗼 ⚡ signaling state changed: peer connection disconnected 🔴")
                        terminate()
                    }
                    is PeerConnectionEvent.Failed -> {
                        Timber.d("🗼 ⚡ signaling state changed: peer connection failed ❌")
                        terminateWithError()
                    }
                }
            }
            .catch { exception ->
                Timber.e("🗼 ⚡ an exception occurred: ${exception.localizedMessage}")
                terminateWithError()
            }
            .completeWhenDisconnected()
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)
    }

    private suspend fun createAndSendAnswerToRemoteClient() {
        webRtcManager.createAnswer()
            .onSuccess { sessionDescriptionValue ->
                val localSessionDescription = SessionDescriptionWrapper(
                    type = SessionDescriptionWrapper.Type.ANSWER,
                    sessionDescriptionValue = sessionDescriptionValue
                )
                // first set the local session description
                val isSet = setLocalDescription(
                    localSessionDescription = localSessionDescription
                )
                if (isSet) {
                    // then send the answer to the connector extension via signaling server
                    Timber.d("🗼 \uD83D\uDCE1️ send answer to the connector extension ⬆️")
                    webSocketClient.sendAnswerMessage(
                        remoteClientId = "",
                        answerPayload = sessionDescriptionValue.toAnswerPayload()
                    )
                } else {
                    terminateWithError()
                }
            }
            .onFailure { throwable ->
                Timber.e("🗼️ failed to create answer: ${throwable.message}❗")
                terminateWithError()
            }
    }

    private suspend fun setLocalDescription(localSessionDescription: SessionDescriptionWrapper): Boolean {
        return webRtcManager.setLocalDescription(localSessionDescription).isSuccess
    }

    private suspend fun setRemoteDescriptionFromOffer(offer: SignalingServerMessage.RemoteData.Offer) {
        val sessionDescription = SessionDescriptionWrapper(
            type = SessionDescriptionWrapper.Type.OFFER,
            sessionDescriptionValue = SessionDescriptionWrapper.SessionDescriptionValue(offer.sdp)
        )
        webRtcManager.setRemoteDescription(sessionDescription)
    }

    private suspend fun sendIceCandidateToRemoteClient(iceCandidateData: PeerConnectionEvent.IceCandidate.Data) {
        Timber.d("🗼️ \uD83D\uDCE1️ send ice candidate to the connector extension ⬆️")
        withContext(ioDispatcher) {
            webSocketClient.sendIceCandidateMessage(
                remoteClientId = "",
                iceCandidateData = iceCandidateData
            )
        }
    }

    private suspend fun addRemoteIceCandidateInWebRtc(iceCandidate: SignalingServerMessage.RemoteData.IceCandidate) {
        val remoteIceCandidate = iceCandidate.remoteIceCandidate
        webRtcManager.addRemoteIceCandidate(remoteIceCandidate = remoteIceCandidate)
    }

    private suspend fun terminateWithError() {
        webSocketClientJob?.cancel()
        webSocketClient.closeSession()
        webRtcManagerJob?.cancel()
        webRtcManager.close()
        addConnectionDeferred.complete(Result.failure(Throwable("data channel couldn't initialize")))
    }

    private suspend fun terminate() {
        Timber.d("🗼️ terminate webrtc and web socket connection \uD83D\uDEAB")
        webSocketClientJob?.cancel()
        webSocketClient.closeSession()
        webRtcManagerJob?.cancel()
        webRtcManager.close()
        addConnectionDeferred.complete(Result.success(Unit))
    }
}
