package rdx.works.peerdroid.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.data.webrtc.model.completeWhenDisconnected
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.AnswerPayload.Companion.toAnswerPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.helpers.sha256
import rdx.works.peerdroid.helpers.toHexString
import timber.log.Timber

interface PeerdroidConnector {

    /**
     * Call this function to add a connection in the wallet settings.
     *
     */
    suspend fun addConnection(encryptionKey: ByteArray): Result<Unit>

    /**
     * Call this function to get a WebRTC data channel.
     * It requires an existing connection in the wallet settings.
     *
     */
    suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper>

    suspend fun close(shouldCloseConnectionToSignalingServer: Boolean = false)
}

/*
 * PeerdroidConnector flow in summary:
 * 1. WebSocketClient connect to signaling server
 * 2. WebRtcManager initialize
 * 3. WebRtcManager create offer: local session description
 * 4. WebRtcManager set local session description in its WebRTC
 * 5. WebSocketClient sends offer to the extension
 * 6. WebRtcManager starts collecting ice candidates and send them to the extension one by one
 * 7. WebSocketClient receives answer from the extension: remote session description
 * 8. WebRtcManager set remote session description in its WebRTC
 * 9. WebSocketClient receives remote ice candidates once by one from the extension
 * 10. WebRtcManager set remote ice candidates one by one in its WebRTC
 *
 * Once the peer connection state changes (observeWebRtcEvents) to CONNECTED
 * it means the data channel is open therefor the PeerdroidConnector completes its flow
 * and returns the data channel (dataChannelDeferred).
 */
@Suppress("TooManyFunctions")
internal class PeerdroidConnectorImpl(
    private val webRtcManager: WebRtcManager,
    private val webSocketClient: WebSocketClient, // to talk to the signaling sever
    @ApplicationScope private val applicationScope: CoroutineScope, // TODO we might need to pass another scope here
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidConnector {

    private var observePeerConnectionJob: Job? = null
    private var observeWebSocketJob: Job? = null
    private var sendIceCandidatesJob: Job? = null

    // This CompletableDeferred will return a result when a peer connection
    // has been first connected and then disconnected.
    private lateinit var addConnectionDeferred: CompletableDeferred<Result<Unit>>

    // This CompletableDeferred will return the data channel.
    // if the whole flow is complete (step 10) and no errors occurred will return in a Result.Success
    // if any error occurred during the flow will return a Result.Error along with an error message.
    private lateinit var dataChannelDeferred: CompletableDeferred<Result<DataChannelWrapper>>

    override suspend fun addConnection(encryptionKey: ByteArray): Result<Unit> {
        Timber.d("⚙️ add new connection")
        addConnectionDeferred = CompletableDeferred()
        // get connection id from encryption key
        val connectionId = encryptionKey.sha256().toHexString()

        withContext(ioDispatcher) {
            val result = webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
            when (result) {
                is Result.Success -> {
                    observePeerConnectionUntilEstablished()
                    listenForIncomingMessagesFromSignalingServer()
                }
                is Result.Error -> {
                    addConnectionDeferred.complete(Result.Error("failed to establish websocket client"))
                }
            }
        }

        return addConnectionDeferred.await()
    }

    override suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper> {
        Timber.d("⚙️ initialize data channel")
        dataChannelDeferred = CompletableDeferred()
        // get connection id from encryption key
        val connectionId = encryptionKey.sha256().toHexString()

        withContext(ioDispatcher) {
            val result = webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
            when (result) {
                is Result.Success -> {
                    observePeerConnectionEvents()
                    listenForIncomingMessagesFromSignalingServer()
                }
                is Result.Error -> {
                    dataChannelDeferred.complete(Result.Error("failed to establish websocket client"))
                }
            }
        }

        return dataChannelDeferred.await()
    }

    // a peer connection executed its lifecycle:
    // created -> connecting -> connected -> disconnected
    private fun observePeerConnectionUntilEstablished() {
        webRtcManager
            .createPeerConnection()
            .onStart { // for debugging
                Timber.d("⚙️ 🛠️ start observing webrtc events")
            }
            .onCompletion { // for debugging
                Timber.d("⚙️ 🛠️ end observing webrtc events")
            }
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.d("⚙️ 🛠️ renegotiation needed")
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("⚙️ 🛠️ ice gathering state changed: ${event.state}")
                    }
                    is PeerConnectionEvent.IceCandidate -> {
                        sendIceCandidateToRemotePeer(event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Connected -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: peer connection connected 🟢")
                    }
                    PeerConnectionEvent.Disconnected -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: peer connection disconnected 🔴")
                        addConnectionDeferred.complete(Result.Success(Unit))
                    }
                    PeerConnectionEvent.Failed -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: peer connection failed ❌")
                        addConnectionDeferred.complete(Result.Error(message = "peer connection failed"))
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ 🛠️ an exception occurred: ${exception.localizedMessage}")
                addConnectionDeferred.complete(Result.Error(message = exception.localizedMessage))
            }
            .completeWhenDisconnected()
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)
    }

    private fun observePeerConnectionEvents() {
        observePeerConnectionJob = webRtcManager
            .createPeerConnection()
            .onStart { // for debugging
                Timber.d("⚙️ ⚡ start observing webrtc events")
            }
            .onCompletion { // for debugging
                Timber.d("⚙️ ⚡ end observing webrtc events")
            }
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.d("⚙️ ⚡ renegotiation needed")
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("⚙️ ⚡ ice gathering state changed: ${event.state}")
                    }
                    is PeerConnectionEvent.IceCandidate -> {
                        sendIceCandidateToRemotePeer(event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("⚙️ ⚡ signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Connected -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: peer connection connected 🟢")
                        dataChannelDeferred.complete(
                            Result.Success(
                                data = DataChannelWrapper(webRtcDataChannel = webRtcManager.getDataChannel())
                            )
                        )
                    }
                    PeerConnectionEvent.Disconnected -> {
                        Timber.d("⚙️ 🛠️ signaling state changed: peer connection disconnected 🔴")
                    }
                    PeerConnectionEvent.Failed -> {
                        Timber.d("⚙️ ⚡ signaling state changed: peer connection failed ❌")
                        dataChannelDeferred.complete(Result.Error(message = "peer connection failed"))
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ ⚡ an exception occurred: ${exception.localizedMessage}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
            .flowOn(ioDispatcher)
            .cancellable()
            .launchIn(applicationScope)
    }

    @Suppress("LongMethod")
    private fun listenForIncomingMessagesFromSignalingServer() {
        observeWebSocketJob = webSocketClient
            .observeMessages()
            .onStart { // for debugging
                Timber.d("⚙️ 📩 start observing incoming messages from signaling server")
            }
            .onCompletion { // for debugging
                Timber.d("⚙️ 📩 end observing incoming messages from signaling server")
            }
            .onEach { incomingMessage ->
                when (incomingMessage) {
                    is SignalingServerMessage.RemoteData.Offer -> {
                        Timber.d("⚙️ 📩  remote client offer received")
                        setRemoteDescriptionFromOffer(incomingMessage)
                        createAndSendAnswer()
                    }
                    is SignalingServerMessage.RemoteData.Answer -> {
                        Timber.d("⚙️ 📩 remote client answer received")
                    }
                    is SignalingServerMessage.RemoteData.IceCandidate -> {
                        Timber.d("⚙️ 📩 received ice candidate from remote client")
                        addRemoteIceCandidateInWebRtc(incomingMessage)
                    }
                    is SignalingServerMessage.Confirmation -> {
                        Timber.d("⚙️ 📩 confirmation received")
                    }
                    is SignalingServerMessage.Error.InvalidMessage -> {
                        Timber.d("⚙️ 📩 invalid message error: ${incomingMessage.errorMessage}")
                    }
                    is SignalingServerMessage.RemoteInfo.MissingClient -> {
                        Timber.d("⚙️ 📩 missing remote client error, request id: ${incomingMessage.requestId}")
                    }
                    is SignalingServerMessage.RemoteInfo.ClientDisconnected -> {
                        Timber.d("⚙️ 📩 remote client disconnected: ${incomingMessage.remoteClientId}")
                    }
                    is SignalingServerMessage.RemoteInfo.ClientConnected -> {
                        Timber.d("⚙️ 📩 remote client is connected: ${incomingMessage.remoteClientId}")
                    }
                    SignalingServerMessage.Error.Validation -> {
                        Timber.d("⚙️ 📩 validation error")
                        terminate(isDuringNegotiation = true) // TODO do we need it?
                    }
                    SignalingServerMessage.Error.Unknown -> {
                        Timber.d("⚙️ 📩 unknown error")
                        terminate(isDuringNegotiation = true)
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ 📩 an exception occurred: ${exception.localizedMessage}")
            }
            .flowOn(ioDispatcher)
            .cancellable()
            .launchIn(applicationScope)
    }

    private suspend fun createAndSendAnswer() {
        when (val result = webRtcManager.createAnswer()) {
            is Result.Success -> {
                val sessionDescriptionValue = result.data
                val localSessionDescription = SessionDescriptionWrapper(
                    type = SessionDescriptionWrapper.Type.ANSWER,
                    sessionDescriptionValue = sessionDescriptionValue
                )
                // first set the local session description
                val isSet = setLocalDescription(
                    localSessionDescription = localSessionDescription
                )
                if (isSet) {
                    // then send the offer to the extension via signaling server
                    Timber.d("⚙️ send answer to the extension")
                    webSocketClient.sendAnswerMessage(
                        answerPayload = sessionDescriptionValue.toAnswerPayload()
                    )
                } else {
                    Timber.e("⚙️ failed to set local description")
                    dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
                }
            }
            is Result.Error -> {
                Timber.e("⚙️ failed to create answer: ${result.message}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
        }
    }

    private suspend fun setLocalDescription(
        localSessionDescription: SessionDescriptionWrapper
    ): Boolean {
        Timber.d("⚙️ set local session description in local WebRTC")
        return when (val result = webRtcManager.setLocalDescription(localSessionDescription)) {
            is Result.Success -> {
                Timber.d("⚙️ local description set, now start observing the data channel state")
                true
            }
            is Result.Error -> {
                Timber.e("⚙️ failed to set local description:${result.message} ")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
                false
            }
        }
    }

    private suspend fun setRemoteDescriptionFromOffer(offer: SignalingServerMessage.RemoteData.Offer) {
        val sessionDescription = SessionDescriptionWrapper(
            type = SessionDescriptionWrapper.Type.OFFER,
            sessionDescriptionValue = SessionDescriptionValue(offer.sdp)
        )
        Timber.d("⚙️ set remote session description in local WebRTC")
        webRtcManager.setRemoteDescription(sessionDescription)
    }

    private fun sendIceCandidateToRemotePeer(iceCandidateData: PeerConnectionEvent.IceCandidate.Data) {
        sendIceCandidatesJob = applicationScope.launch(ioDispatcher) {
            ensureActive()
            webSocketClient.sendIceCandidateMessage(
                iceCandidateData = iceCandidateData
            )
        }
    }

    private suspend fun addRemoteIceCandidateInWebRtc(iceCandidate: SignalingServerMessage.RemoteData.IceCandidate) {
        val remoteIceCandidate = iceCandidate.remoteIceCandidate
        Timber.d("⚙️ set remote ice candidate in local WebRTC")
        webRtcManager.addRemoteIceCandidate(remoteIceCandidate = remoteIceCandidate)
    }

    override suspend fun close(shouldCloseConnectionToSignalingServer: Boolean) {
        if (shouldCloseConnectionToSignalingServer) {
            terminate()
            return
        }
        Timber.d("⚙️ close webrtc but keep open web socket connection")
        sendIceCandidatesJob?.cancel()
        observePeerConnectionJob?.cancel()
    }

    private suspend fun terminate(isDuringNegotiation: Boolean = false) {
        Timber.d("⚙️ terminate webrtc and web socket connection")
        observePeerConnectionJob?.cancel()
        sendIceCandidatesJob?.cancel()
        observeWebSocketJob?.cancel()
        webSocketClient.closeSession()
        if (isDuringNegotiation) {
            dataChannelDeferred.complete(Result.Error("an error occurred"))
        }
    }
}
