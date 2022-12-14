package rdx.works.peerdroid.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.IceCandidatePayload.Companion.toJsonArrayPayload
import rdx.works.peerdroid.data.websocket.model.RpcMessage.OfferPayload.Companion.toPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerIncomingMessage
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.helpers.sha256
import rdx.works.peerdroid.helpers.toHexString
import timber.log.Timber

interface PeerdroidConnector {

    suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper>

    suspend fun close(
        shouldCloseConnectionToSignalingServer: Boolean = false
    )
}

/*
 * PeerdroidConnector flow in summary:
 * 1. WebSocketClient connect to signaling server
 * 2. WebRtcManager initialize
 * 3. WebRtcManager starts collecting ice candidates
 * 4. WebRtcManager create offer: local session description
 * 5. WebRtcManager set local session description in its WebRTC
 * 6. WebSocketClient sends offer to the extension
 * 7. WebSocketClient receives answer from the extension: remote session description
 * 8. WebRtcManager set remote session description in its WebRTC
 * 9. WebSocketClient sends local ice candidates to the extension
 * 10. WebSocketClient receives remote ice candidates from the extension
 * 11. WebRtcManager set remote ice candidates in its WebRTC
 *
 * Once the peer connection state changes (observeWebRtcEvents) to CONNECTED
 * it means the data channel is open therefor the PeerdroidConnector completes its flow
 * and returns the data channel (dataChannelDeferred).
 */
internal class PeerdroidConnectorImpl(
    private val webRtcManager: WebRtcManager,
    private val webSocketClient: WebSocketClient, // to talk to the signaling sever
    @ApplicationScope private val applicationScope: CoroutineScope, // TODO we might need to pass another scope here
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidConnector {

    private var webRtcJob: Job? = null
    private var webSocketJob: Job? = null
    private var iceCandidatesJob: Job? = null

    // used as parameter for the web socket connection
    // and for as a label name of the WebRTC data channel
    private lateinit var connectionId: String

    // This CompletableDeferred will return the data channel.
    // if the whole flow is complete (step 11) and no errors occurred will return in a Result.Success
    // if any error occurred during the flow will return a Result.Error along with an error message.
    private lateinit var dataChannelDeferred: CompletableDeferred<Result<DataChannelWrapper>>

    // here we collect the local ice candidates
    private val localIceCandidatesList = mutableListOf<PeerConnectionEvent.IceCandidate.Data>()

    override suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper> {
        Timber.d("initialize data channel")
        dataChannelDeferred = CompletableDeferred()
        // get connection id from encryption key
        this.connectionId = encryptionKey.sha256().toHexString()

        withContext(ioDispatcher) {
            val result = webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
            when (result) {
                is Result.Success -> {
                    listenForIncomingMessagesFromSignalingServer()
                    observeWebRtcEvents()
                }
                is Result.Error -> {
                    dataChannelDeferred.complete(Result.Error("failed to establish websocket client"))
                }
            }
        }

        return dataChannelDeferred.await()
    }

    private fun observeWebRtcEvents() {
        webRtcJob = webRtcManager
            .createPeerConnection(connectionId)
            .cancellable()
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.d("renegotiation needed")
                        createAndSendOffer()
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("ice gathering state changed: ${event.state}")
                        when (event.state) {
                            PeerConnectionEvent.IceGatheringChange.State.GATHERING -> {
                                waitForIceCandidatesAndSend()
                            }
                            PeerConnectionEvent.IceGatheringChange.State.COMPLETE -> {}
                            PeerConnectionEvent.IceGatheringChange.State.NEW -> {}
                            PeerConnectionEvent.IceGatheringChange.State.UNKNOWN -> {}
                        }
                    }
                    is PeerConnectionEvent.IceCandidate -> { // triggered during the IceGatheringChangeEvent.State = GATHERING
                        localIceCandidatesList.add(event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Connected -> {
                        dataChannelDeferred.complete(
                            Result.Success(
                                data = DataChannelWrapper(webRtcDataChannel = webRtcManager.getDataChannel())
                            )
                        )
                    }
                    PeerConnectionEvent.Disconnected -> {
                        Timber.d("signaling state changed: peer connection disconnected")
                    }
                }
            }
            .catch { exception ->
                Timber.e("an exception occurred: ${exception.localizedMessage}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)
    }

    private fun listenForIncomingMessagesFromSignalingServer() {
        webSocketJob = webSocketClient
            .observeMessages()
            .cancellable()
            .onEach { incomingMessage ->
                when (incomingMessage) {
                    is SignalingServerIncomingMessage.BrowserExtensionAnswer -> {
                        Timber.d("remote client answer received")
                        setRemoteDescriptionFromAnswer(incomingMessage)
                    }
                    is SignalingServerIncomingMessage.BrowserExtensionIceCandidates -> {
                        Timber.d("remote client ice candidates received")
                        addRemoteIceCandidatesInWebRtc(incomingMessage)
                    }
                    is SignalingServerIncomingMessage.Confirmation -> {
                        Timber.d("confirmation received")
                    }
                    is SignalingServerIncomingMessage.InvalidMessageError -> {
                        Timber.d("invalid message error: ${incomingMessage.errorMessage}")
                    }
                    is SignalingServerIncomingMessage.MissingRemoteClientError -> {
                        Timber.d("missing remote client error, request id: ${incomingMessage.requestId}")
                    }
                    SignalingServerIncomingMessage.RemoteClientDisconnected -> {
                        Timber.d("remote client disconnected")
                    }
                    SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected -> {
                        Timber.d("remote client is already connected")
                    }
                    SignalingServerIncomingMessage.RemoteClientJustConnected -> {
                        Timber.d("remote client just connected")
                    }
                    SignalingServerIncomingMessage.RemoteClientSourceError -> {
                        Timber.d("remote client source error")
                        terminatePeerdroidConnectorWithError()
                    }
                    SignalingServerIncomingMessage.RemoteConnectionIdNotMatchedError -> {
                        Timber.d("remote connection id not matched")
                        terminatePeerdroidConnectorWithError()
                    }
                    SignalingServerIncomingMessage.ValidationError -> {
                        Timber.d("validation error")
                    }
                    SignalingServerIncomingMessage.UnknownMessage -> {
                        Timber.d("unknown incoming message")
                        terminatePeerdroidConnectorWithError()
                    }
                    SignalingServerIncomingMessage.UnknownError -> {
                        Timber.d("unknown error")
                        terminatePeerdroidConnectorWithError()
                    }
                }
            }
            .catch { exception ->
                Timber.e("an exception occurred: ${exception.localizedMessage}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)
    }

    private suspend fun createAndSendOffer() {
        when (val result = webRtcManager.createOffer()) {
            is Result.Success -> {
                val sessionDescriptionValue = result.data
                val localSessionDescription = SessionDescriptionWrapper(
                    type = SessionDescriptionWrapper.Type.OFFER,
                    sessionDescriptionValue = sessionDescriptionValue
                )
                // first set the local session description
                val isSet = setLocalDescriptionFromOfferAndObserveDataChannelState(
                    localSessionDescription = localSessionDescription
                )
                if (isSet) {
                    // then send the offer to the extension via signaling server
                    Timber.d("send offer to the extension")
                    webSocketClient.sendOfferMessage(sessionDescriptionValue.toPayload())
                } else {
                    Timber.e("failed to set local description")
                    dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
                }
            }
            else -> {
                Timber.d("failed to create offer")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
        }
    }

    private suspend fun setLocalDescriptionFromOfferAndObserveDataChannelState(
        localSessionDescription: SessionDescriptionWrapper
    ): Boolean {
        Timber.d("set local session description in local WebRTC")
        return when (webRtcManager.setLocalDescription(localSessionDescription)) {
            is Result.Success -> {
                Timber.d("local description set, now start observing the data channel state")
                true
            }
            is Result.Error -> {
                Timber.e("failed to set local description")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
                false
            }
        }
    }

    private suspend fun setRemoteDescriptionFromAnswer(
        browserExtensionAnswer: SignalingServerIncomingMessage.BrowserExtensionAnswer
    ) {
        val sessionDescription = SessionDescriptionWrapper(
            type = SessionDescriptionWrapper.Type.ANSWER,
            sessionDescriptionValue = SessionDescriptionValue(browserExtensionAnswer.sdp)
        )
        Timber.d("set remote session description in local WebRTC")
        webRtcManager.setRemoteDescription(sessionDescription)
    }

    // since this function must delay for 1 second,
    // then launch it in a separate coroutine in order to not block the observeWebRtcEvents flow
    private fun waitForIceCandidatesAndSend() {
        iceCandidatesJob = applicationScope.launch(ioDispatcher) {
            delay(ONE_SECOND_TO_COLLECT_ICE_CANDIDATES)
            if (localIceCandidatesList.isEmpty()) {
                Timber.d("no ice candidates collected")
            }

            Timber.d("send ${localIceCandidatesList.size} ice candidates to the extension")
            ensureActive()
            webSocketClient.sendIceCandidatesMessage(localIceCandidatesList.toJsonArrayPayload())
            localIceCandidatesList.clear()
        }
    }

    private suspend fun addRemoteIceCandidatesInWebRtc(
        browserExtensionIceCandidates: SignalingServerIncomingMessage.BrowserExtensionIceCandidates
    ) {
        val remoteIceCandidates = browserExtensionIceCandidates.remoteIceCandidates
        Timber.d("set ${remoteIceCandidates.size} remote ice candidates in local WebRTC")
        webRtcManager.addRemoteIceCandidates(
            remoteIceCandidates = remoteIceCandidates
        )
    }

    override suspend fun close(shouldCloseConnectionToSignalingServer: Boolean) {
        if (shouldCloseConnectionToSignalingServer) {
            terminate()
            return
        }
        Timber.d("close")
        iceCandidatesJob?.cancel()
        webRtcJob?.cancel()
        localIceCandidatesList.clear()
    }

    private suspend fun terminatePeerdroidConnectorWithError() {
        terminate()
        dataChannelDeferred.complete(Result.Error("an error occurred"))
    }

    private suspend fun terminate() {
        Timber.d("terminate")
        webRtcJob?.cancel()
        iceCandidatesJob?.cancel()
        webSocketClient.closeSession()
        webSocketJob?.cancel()
    }

    companion object {
        private const val ONE_SECOND_TO_COLLECT_ICE_CANDIDATES = 1000L
    }
}
