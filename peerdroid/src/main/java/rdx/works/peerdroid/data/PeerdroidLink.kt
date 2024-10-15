package rdx.works.peerdroid.data

import android.content.Context
import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.PublicKey
import com.radixdlt.sargon.Signature
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rdx.works.core.sargon.serializers.SargonPublicKeySerializer
import rdx.works.core.sargon.serializers.SargonSignatureSerializer
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.completeWhenDisconnected
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.AnswerPayload.Companion.toAnswerPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

interface PeerdroidLink {

    /**
     * Initiates a p2p connection and the linking flow using [P2pLink.connectionPassword].
     * Lets the caller complete the linking through [ConnectionListener] callback
     * by computing [LinkClientExchangeInteraction] message and sending it using [sendMessage].
     * Upon successful message sending, the linking is considered completed.
     */
    suspend fun addConnection(
        p2pLink: P2pLink,
        connectionListener: ConnectionListener
    ): Result<Unit>

    suspend fun sendMessage(
        connectionId: String,
        message: LinkClientExchangeInteraction
    ): Result<Unit>

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class LinkClientExchangeInteraction(
        @EncodeDefault
        @SerialName("discriminator")
        val discriminator: String = "linkClient",
        @Serializable(with = SargonPublicKeySerializer::class)
        @SerialName("publicKey")
        val publicKey: PublicKey,
        @Serializable(with = SargonSignatureSerializer::class)
        @SerialName("signature")
        val signature: Signature
    )

    interface ConnectionListener {

        suspend fun completeLinking(connectionId: String): Result<Unit>
    }
}

@Suppress("TooManyFunctions")
internal class PeerdroidLinkImpl(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidLink {

    private val webSocketClient = WebSocketClient(applicationContext)
    private val webRtcManager = WebRtcManager(applicationContext)

    private var webSocketClientJob: Job? = null
    private var webRtcManagerJob: Job? = null

    // A successful link connection between wallet and connector extensions happens when with the connection id
    // a peer connection is successfully established and gets terminated instantly.
    // Success flow: connection to websocket with connectionId -> peer connection created -> connecting -> connected -> disconnected
    // This CompletableDeferred will return a result indicating the above result.
    private lateinit var addConnectionDeferred: CompletableDeferred<Result<Unit>>

    // This CompletableDeferred will return a result indicating if the peer connection is ready or not.
    private lateinit var peerConnectionDeferred: CompletableDeferred<Result<Unit>>

    override suspend fun addConnection(
        p2pLink: P2pLink,
        connectionListener: PeerdroidLink.ConnectionListener
    ): Result<Unit> {
        addConnectionDeferred = CompletableDeferred()
        peerConnectionDeferred = CompletableDeferred()

        // get connection id from encryption key
        val connectionId = ConnectionIdHolder(p2pLink)
        Timber.tag("LinkingCE").d("\uD83D\uDDFC️ start process to add a new link connector with connectionId: $connectionId")

        withContext(ioDispatcher) {
            observePeerConnectionUntilEstablished(connectionId.id, connectionListener)
            peerConnectionDeferred.await() // wait until the peer connection is initialized and ready to negotiate
            // and now establish the web socket
            webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = p2pLink.connectionPassword
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

    override suspend fun sendMessage(
        connectionId: String,
        message: PeerdroidLink.LinkClientExchangeInteraction
    ): Result<Unit> {
        val dataChannelWrapper = DataChannelWrapper(
            connectionIdHolder = ConnectionIdHolder(connectionId),
            webRtcDataChannel = webRtcManager.getDataChannel()
        )

        val serializedMessage = Json.encodeToString(message)
        Timber.tag("LinkingCE").d("🗼 \uD83D\uDCE1️ sending message to the connector extension ⬆️")
        return dataChannelWrapper.sendMessage(serializedMessage)
    }

    @Suppress("LongMethod", "MaximumLineLength", "MaxLineLength")
    private fun listenForIncomingMessagesFromSignalingServer(webSocketClient: WebSocketClient) {
        webSocketClientJob = webSocketClient
            .listenForMessages()
            .onStart { // for debugging
                Timber.tag("LinkingCE").d("\uD83D\uDDFC start observing incoming messages from signaling server ▶️️")
            }
            .onCompletion {
                Timber.tag("LinkingCE").d("\uD83D\uDDFC️️ end observing incoming messages from signaling server ⏹️")
            }
            .onEach { incomingMessage ->
                when (incomingMessage) {
                    is SignalingServerMessage.RemoteInfo.ClientConnected -> {
                        Timber.tag("LinkingCE").d(
                            "🗼️ \uD83D\uDCE1️ connector extension is connected with id: ${incomingMessage.remoteClientId} ⬇️ \uD83D\uDFE9"
                        )
                    }
                    is SignalingServerMessage.RemoteData.Offer -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ offer received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                        setRemoteDescriptionFromOffer(incomingMessage)
                        createAndSendAnswerToRemoteClient()
                    }
                    is SignalingServerMessage.RemoteData.Answer -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ answer received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                    }
                    is SignalingServerMessage.RemoteData.IceCandidate -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ ice candidate received from connector extension: ${incomingMessage.remoteClientId} ⬇️")
                        addRemoteIceCandidateInWebRtc(incomingMessage)
                    }
                    is SignalingServerMessage.Confirmation -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ confirmation received for requestId: ${incomingMessage.requestId} ⬇️")
                    }
                    is SignalingServerMessage.Error.InvalidMessage -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ invalid message error: ${incomingMessage.errorMessage} ⬇️")
                    }
                    is SignalingServerMessage.RemoteInfo.MissingClient -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ missing connector extension error, request id: ${incomingMessage.requestId} ⬇️")
                    }
                    is SignalingServerMessage.RemoteInfo.ClientDisconnected -> {
                        Timber.tag("LinkingCE").d(
                            "🗼️ \uD83D\uDCE1️️ connector extension disconnected with id: ${incomingMessage.remoteClientId} ⬇️ \uD83D\uDFE5"
                        )
                    }
                    is SignalingServerMessage.Error.Validation -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ validation error ❗ ⬇️")
                        terminateWithError()
                    }
                    is SignalingServerMessage.Error.Unknown -> {
                        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️️ unknown error ❗ ⬇️")
                        terminateWithError()
                    }
                }
            }
            .catch { exception ->
                Timber.tag("LinkingCE").e("🗼️ ⬇️ an exception occurred: ${exception.localizedMessage}")
                terminateWithError()
            }
            .flowOn(ioDispatcher)
            .cancellable()
            .launchIn(applicationScope)
    }

    // a peer connection executed its lifecycle:
    // created -> connecting -> connected -> disconnected
    private fun observePeerConnectionUntilEstablished(
        connectionId: String,
        connectionListener: PeerdroidLink.ConnectionListener
    ) {
        webRtcManagerJob = webRtcManager
            .createPeerConnection("")
            .onStart { // for debugging
                Timber.tag("LinkingCE").d("🗼 ⚡ start observing webrtc events ▶️")
            }
            .onCompletion { // for debugging
                Timber.tag("LinkingCE").d("🗼 ⚡ end observing webrtc events ⏹️")
            }
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ renegotiation needed 🆗")
                        peerConnectionDeferred.complete(Result.success(Unit))
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ ice gathering state changed: ${event.state}")
                    }
                    is PeerConnectionEvent.IceCandidate -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ ice candidate generated")
                        sendIceCandidateToRemoteClient(event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Connected -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ signaling state changed: peer connection connected 🟢")

                        // give some time to ensure the CE got the connected event too and
                        // started listening the data channel
                        delay(0.5.seconds)
                        connectionListener.completeLinking(connectionId)
                            .onSuccess {
                                Timber.tag("LinkingCE").d("🗼️ linking completed")
                                terminateWithSuccess()
                            }
                            .onFailure { throwable ->
                                Timber.tag("LinkingCE").e("🗼️ failed to complete linking: ${throwable.message}❗")
                                terminateWithError()
                            }
                    }
                    is PeerConnectionEvent.Disconnected -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ signaling state changed: peer connection disconnected 🔴")
                        terminateWithError()
                    }
                    is PeerConnectionEvent.Failed -> {
                        Timber.tag("LinkingCE").d("🗼 ⚡ signaling state changed: peer connection failed ❌")
                        terminateWithError()
                    }
                }
            }
            .catch { exception ->
                Timber.tag("LinkingCE").e("🗼 ⚡ an exception occurred: ${exception.localizedMessage}")
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
                    Timber.tag("LinkingCE").d("🗼 \uD83D\uDCE1️ send answer to the connector extension ⬆️")
                    webSocketClient.sendAnswerMessage(
                        remoteClientId = "",
                        answerPayload = sessionDescriptionValue.toAnswerPayload()
                    )
                } else {
                    terminateWithError()
                }
            }
            .onFailure { throwable ->
                Timber.tag("LinkingCE").e("🗼️ failed to create answer: ${throwable.message}❗")
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
        Timber.tag("LinkingCE").d("🗼️ \uD83D\uDCE1️ send ice candidate to the connector extension ⬆️")
        webSocketClient.sendIceCandidateMessage(
            remoteClientId = "",
            iceCandidateData = iceCandidateData
        )
    }

    private suspend fun addRemoteIceCandidateInWebRtc(iceCandidate: SignalingServerMessage.RemoteData.IceCandidate) {
        val remoteIceCandidate = iceCandidate.remoteIceCandidate
        webRtcManager.addRemoteIceCandidate(remoteIceCandidate = remoteIceCandidate)
    }

    private suspend fun terminateWithError() {
        terminateConnection()
        peerConnectionDeferred.complete(Result.failure(IllegalStateException("peer connection couldn't initialize")))
        addConnectionDeferred.complete(Result.failure(IllegalStateException("data channel couldn't initialize")))
    }

    private suspend fun terminateWithSuccess() {
        terminateConnection()
        addConnectionDeferred.complete(Result.success(Unit))
    }

    private suspend fun terminateConnection() {
        Timber.tag("LinkingCE").d("🗼️ terminate webrtc and web socket connection \uD83D\uDEAB")
        webSocketClientJob?.cancel()
        webSocketClient.closeSession()
        webRtcManagerJob?.cancel()
        webRtcManager.close()
    }
}
