package rdx.works.peerdroid.data

import android.util.Log
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.stateFlow
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.IceCandidatePayload.Companion.toJsonArrayPayload
import rdx.works.peerdroid.data.websocket.model.RpcMessage.OfferPayload.Companion.toPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerIncomingMessage
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result

interface PeerdroidConnector {

    suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper>

    suspend fun close()
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
 * At the time the PeerdroidConnector set the remote session description,
 * it also starts observing the state of the data channel.
 * Once the data channel is open the flow stops and the PeerdroidConnector returns the data channel.
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

    // This CompletableDeferred will return the data channel.
    // if the whole flow is complete (step 11) and no errors occurred will return in a Result.Success
    // if any error occurred during the flow will return a Result.Error along with an error message.
    private lateinit var dataChannelDeferred: CompletableDeferred<Result<DataChannelWrapper>>

    // here we collect the local ice candidates
    private val localIceCandidatesList = mutableListOf<PeerConnectionEvent.IceCandidate.Data>()

    override suspend fun createDataChannel(encryptionKey: ByteArray): Result<DataChannelWrapper> {
        Log.d("CONNECTOR_WEB_RTC", "initialize data channel")
        dataChannelDeferred = CompletableDeferred()

        withContext(ioDispatcher) {
            val result = webSocketClient.initSession(
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

    // this will be observing the state of the data channel until is open: `takeWhile { isOpen`
    // once it is open it will return the dataChannelDeferred with the data channel
    private fun observeDataChannelStateUntilOpen(dataChannel: DataChannel) {
        dataChannel.stateFlow()
            .takeWhile { isOpen ->
                dataChannelDeferred.complete(
                    Result.Success(
                        data = DataChannelWrapper(webRtcDataChannel = dataChannel)
                    )
                )
                isOpen
            }.onCompletion {
                // unregister the observer because we will register
                // a new one (eventFlow) for incoming messages and state changes
                dataChannel.unregisterObserver()
            }
            .launchIn(applicationScope)
    }

    private fun observeWebRtcEvents() {
        webRtcJob = webRtcManager.createPeerConnection()
            .onEach { event ->
                when (event) {
                    PeerConnectionEvent.RenegotiationNeeded -> {
                        Log.d("CONNECTOR_WEB_RTC", "renegotiation needed")
                        createAndSendOffer()
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Log.d("CONNECTOR_WEB_RTC", "ice gathering state changed: ${event.state}")
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
                        Log.d("CONNECTOR_WEB_RTC", "ice candidate collected")
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Log.d("CONNECTOR_WEB_RTC", "signaling state changed: ${event.message}")
                    }
                    PeerConnectionEvent.Disconnected -> {
                        Log.d("CONNECTOR_WEB_RTC", "signaling state changed: peer connection disconnected")
                    }
                }
            }
            .catch { exception ->
                Log.e("CONNECTOR_WEB_RTC", "an exception occurred: ${exception.localizedMessage}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
            .flowOn(ioDispatcher)
            .cancellable()
            .launchIn(applicationScope)
    }

    private fun listenForIncomingMessagesFromSignalingServer() {
        webSocketJob = webSocketClient.observeMessages()
            .onEach { incomingMessage ->
                when (incomingMessage) {
                    is SignalingServerIncomingMessage.BrowserExtensionAnswer -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client answer received")
                        setRemoteDescriptionFromAnswer(incomingMessage)
                    }
                    is SignalingServerIncomingMessage.BrowserExtensionIceCandidates -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client ice candidates received")
                        addRemoteIceCandidatesInWebRtc(incomingMessage)
                    }
                    is SignalingServerIncomingMessage.Confirmation -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "confirmation received")
                    }
                    is SignalingServerIncomingMessage.InvalidMessageError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "invalid message error: ${incomingMessage.errorMessage}")
                    }
                    is SignalingServerIncomingMessage.MissingRemoteClientError -> {
                        Log.d(
                            "CONNECTOR_WEB_SOCKET",
                            "missing remote client error, request id: ${incomingMessage.requestId}"
                        )
                        closePeerdroidConnectorWithError()
                    }
                    SignalingServerIncomingMessage.RemoteClientDisconnected -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client disconnected")
                    }
                    SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client is already connected")
                    }
                    SignalingServerIncomingMessage.RemoteClientJustConnected -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client just connected")
                    }
                    SignalingServerIncomingMessage.RemoteClientSourceError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote client source error")
                    }
                    SignalingServerIncomingMessage.RemoteConnectionIdNotMatchedError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "remote connection id not matched")
                    }
                    SignalingServerIncomingMessage.ValidationError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "validation error")
                    }
                    SignalingServerIncomingMessage.ParsingResponseError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "parsing response error")
                    }
                    SignalingServerIncomingMessage.UnknownMessage -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "unknown incoming message")
                        closePeerdroidConnectorWithError()
                    }
                    SignalingServerIncomingMessage.UnknownError -> {
                        Log.d("CONNECTOR_WEB_SOCKET", "unknown error")
                        closePeerdroidConnectorWithError()
                    }
                }
            }
            .catch { exception ->
                Log.e("CONNECTOR_WEB_SOCKET", "an exception occurred: ${exception.localizedMessage}")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
            .flowOn(ioDispatcher)
            .cancellable()
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
                    Log.d("CONNECTOR_WEB_RTC", "send offer to the extension")
                    webSocketClient.sendOfferMessage(sessionDescriptionValue.toPayload())
                } else {
                    Log.e("CONNECTOR_WEB_SOCKET", "failed to set local description")
                    dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
                }
            }
            else -> {
                Log.d("CONNECTOR", "failed to create offer")
                dataChannelDeferred.complete(Result.Error("data channel couldn't initialize"))
            }
        }
    }

    private suspend fun setLocalDescriptionFromOfferAndObserveDataChannelState(
        localSessionDescription: SessionDescriptionWrapper
    ): Boolean {
        Log.d("CONNECTOR_WEB_RTC", "set local session description in local WebRTC")
        return when (webRtcManager.setLocalDescription(localSessionDescription)) {
            is Result.Success -> {
                Log.d("CONNECTOR_WEB_RTC", "local description set, now start observing the data channel state")
                observeDataChannelStateUntilOpen(
                    dataChannel = webRtcManager.getDataChannel()
                )
                true
            }
            is Result.Error -> {
                Log.e("CONNECTOR_WEB_SOCKET", "failed to set local description")
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
        Log.d("CONNECTOR_WEB_RTC", "set remote session description in local WebRTC")
        webRtcManager.setRemoteDescription(sessionDescription)
    }

    // since this function must delay for 1 second,
    // then launch it in a separate coroutine in order to not block the observeWebRtcEvents flow
    private fun waitForIceCandidatesAndSend() {
        iceCandidatesJob = applicationScope.launch(ioDispatcher) {
            delay(ONE_SECOND_TO_COLLECT_ICE_CANDIDATES)
            if (localIceCandidatesList.isEmpty()) {
                Log.d("CONNECTOR", "no ice candidates collected")
            }

            ensureActive()
            Log.d("CONNECTOR_WEB_RTC", "send ice candidates to the extension")
            webSocketClient.sendIceCandidatesMessage(localIceCandidatesList.toJsonArrayPayload())
        }
    }

    private suspend fun addRemoteIceCandidatesInWebRtc(
        browserExtensionIceCandidates: SignalingServerIncomingMessage.BrowserExtensionIceCandidates
    ) {
        Log.d("CONNECTOR_WEB_RTC", "set remote ice candidates in local WebRTC")
        webRtcManager.addRemoteIceCandidates(
            remoteIceCandidates = browserExtensionIceCandidates.remoteIceCandidates
        )
    }

    private suspend fun closePeerdroidConnectorWithError() {
        close()
        dataChannelDeferred.complete(Result.Error("an error occurred"))
    }

    override suspend fun close() {
        webRtcJob?.cancel() // TODO if you want to restore/restart you might need to keep this open
        iceCandidatesJob?.cancel()
        webSocketClient.closeSession()
        webSocketJob?.cancel()
    }

    companion object {
        private const val ONE_SECOND_TO_COLLECT_ICE_CANDIDATES = 1000L
    }
}
