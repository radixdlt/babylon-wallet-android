@file:Suppress("TooManyFunctions")

package rdx.works.peerdroid.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.blake2Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelWrapper
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage.AnswerPayload.Companion.toAnswerPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage.Confirmation
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage.Error
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage.RemoteData
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage.RemoteInfo
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.PeerConnectionHolder
import rdx.works.peerdroid.domain.RemoteClientHolder
import rdx.works.peerdroid.domain.WebSocketHolder
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

interface PeerdroidConnector {

    val anyChannelConnected: Flow<Boolean>

    suspend fun connectToConnectorExtension(encryptionKey: ByteArray): Result<Unit>

    suspend fun deleteConnector(connectionId: ConnectionIdHolder)

    fun terminateConnectionToConnectorExtension()

    val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent>

    suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit>

    suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit>
}

internal class PeerdroidConnectorImpl(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidConnector {

    // one per connection id (= link connection). One CE can host multiple remote clients (dApps)
    private val mapOfWebSockets = ConcurrentHashMap<ConnectionIdHolder, WebSocketHolder>()

    // TODO change key of the map to ConnectionIdHolder and remove the ConnectionIdHolder from the PeerConnectionHolder
    // one per remote client (dApp)
    private val mapOfPeerConnections = ConcurrentHashMap<RemoteClientHolder, PeerConnectionHolder>()

    // TODO remove the ConnectionIdHolder from the DataChannelHolder
    // One per CE:
    // One DataChannel for each CE (browser) and CE will forward the messages to the right client (dApp) based on the request id.
    private val mapOfDataChannels = ConcurrentHashMap<ConnectionIdHolder, DataChannelHolder>()

    // every time a new data channel opens it will be added in the dataChannelMessagesFromRemoteClients
    private val dataChannelObserver = MutableStateFlow<DataChannelWrapper?>(null)

    private val isAnyChannelConnected = MutableStateFlow(false)

    init {
        Timber.d("⚙️ init PeerdroidConnector")
    }

    override val anyChannelConnected: Flow<Boolean>
        get() = isAnyChannelConnected.asSharedFlow()

    override suspend fun connectToConnectorExtension(encryptionKey: ByteArray): Result<Unit> {
        val connectionId = encryptionKey.blake2Hash().toHexString()

        return withContext(ioDispatcher) {
            if (mapOfWebSockets.containsKey(ConnectionIdHolder(id = connectionId))) {
                Timber.d("⚙️ already tried to establish a link connection with connectionId: $connectionId")
                return@withContext Result.success(Unit)
            }

            Timber.d("⚙️ establishing a link connection with connectionId: $connectionId")
            val webSocketClient = WebSocketClient(applicationContext)
            webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
                .onSuccess {
                    val connectionHolder = ConnectionIdHolder(id = connectionId)
                    val job = listenForMessagesFromRemoteClients(connectionHolder, webSocketClient)
                    mapOfWebSockets[connectionHolder] = WebSocketHolder(
                        webSocketClient = webSocketClient,
                        listenMessagesJob = job
                    )
                }
                .onFailure {
                    Timber.e("⚙️ failed to establish a link connection with connectionId: $connectionId")
                }
        }
    }

    override suspend fun deleteConnector(connectionId: ConnectionIdHolder) {
        Timber.d("⚙️ \uD83D\uDDD1️ delete link connection with connectionId: ${connectionId.id}")
        withContext(ioDispatcher) {
            val webSocketHolder = mapOfWebSockets.remove(connectionId)
            webSocketHolder?.let {
                webSocketHolder.listenMessagesJob.cancel()
                webSocketHolder.webSocketClient.closeSession()
            }
            // close and remove the peer connections and their jobs for this connection id
            val peerConnectionsForTermination = mapOfPeerConnections.values.filter { peerConnectionHolder ->
                peerConnectionHolder.connectionIdHolder == connectionId
            }
            peerConnectionsForTermination.forEach { peerConnectionHolder ->
                peerConnectionHolder.observePeerConnectionJob.cancel()
                peerConnectionHolder.webRtcManager.close()
            }
            mapOfPeerConnections.values.removeAll(peerConnectionsForTermination.toSet())
            // close and remove data channels for this connection id
            val dataChannelsForTermination = mapOfDataChannels.values.filter { dataChannelHolder ->
                dataChannelHolder.connectionIdHolder == connectionId
            }
            dataChannelsForTermination.forEach { dataChannelHolder ->
                dataChannelHolder.dataChannel.close()
            }
            mapOfDataChannels.values.removeAll(dataChannelsForTermination.toSet())
            Timber.d("⚙️ \uD83D\uDDD1️ link connection with connectionId: ${connectionId.id} deleted ✅")
        }
    }

    override fun terminateConnectionToConnectorExtension() {
        applicationScope.launch(ioDispatcher) {
            Timber.d("⚙️ \uD83D\uDEAB terminating all link connections and clear: websockets, peer connections, and data channels")
            mapOfWebSockets.values.forEach { webSocketHolder ->
                webSocketHolder.listenMessagesJob.cancel()
                webSocketHolder.webSocketClient.closeSession()
            }
            mapOfWebSockets.clear()

            mapOfPeerConnections.values.forEach { peerConnectionHolder ->
                peerConnectionHolder.observePeerConnectionJob.cancel()
                peerConnectionHolder.webRtcManager.close()
            }
            mapOfPeerConnections.clear()

            mapOfDataChannels.values.forEach { dataChannelHolder ->
                dataChannelHolder.dataChannel.close()
            }
            mapOfDataChannels.clear()
            Timber.d("⚙️ \uD83D\uDEAB all link connection terminated and cleared ✅")
        }
    }

    // here we merge all the incoming messages
    // from the opened data channels into a single flow
    @OptIn(ExperimentalCoroutinesApi::class)
    override val dataChannelMessagesFromRemoteClients = merge(
        dataChannelObserver
            .mapNotNull {
                it
            }
            .flatMapMerge { dataChannel ->
                dataChannel.dataChannelEvents
            }
            .onCompletion {
                dataChannelObserver.value = null
            }
    ).flowOn(ioDispatcher).shareIn(scope = applicationScope, started = SharingStarted.WhileSubscribed())

    override suspend fun sendDataChannelMessageToRemoteClient(
        remoteConnectorId: String,
        message: String
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            val connectionIdHolder = ConnectionIdHolder(id = remoteConnectorId)
            if (mapOfDataChannels.contains(key = connectionIdHolder)) {
                mapOfDataChannels.getValue(connectionIdHolder).dataChannel.sendMessage(message)
            } else {
                Timber.e("📯 failed to send message to CE: $connectionIdHolder because its data channel is closed❗")
                Result.failure(Throwable("failed to send message to CE"))
            }
        }
    }

    override suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit> {
        return withContext(ioDispatcher) {
            val jobs = mapOfDataChannels.values.map { channel ->
                async { channel.dataChannel.sendMessage(message) }
            }
            val results = jobs.awaitAll()
            if (results.any { it.isSuccess }) {
                Result.success(Unit)
            } else {
                Timber.e("📯 failed to send message to all remote clients❗")
                Result.failure(Throwable("failed to send message to all remote clients"))
            }
        }
    }

    private fun listenForMessagesFromRemoteClients(
        connectionId: ConnectionIdHolder,
        webSocketClient: WebSocketClient
    ): Job {
        val job = applicationScope.launch(ioDispatcher) {
            webSocketClient
                .listenForMessages()
                .collect { signalingServerMessage ->
                    when (signalingServerMessage) {
                        is RemoteInfo -> {
                            if (signalingServerMessage is RemoteInfo.ClientConnected) {
                                Timber.d(
                                    "⚙️ \uD83D\uDCE1️ remote client connected with remoteClientId: " +
                                        "${signalingServerMessage.remoteClientId} ⬇️ \uD83D\uDFE9"
                                )
                                val peerConnectionReadyForNegotiationDeferred = CompletableDeferred<Unit>()
                                val remoteClientHolder = RemoteClientHolder(id = signalingServerMessage.remoteClientId)
                                val peerConnectionHolder = createAndObservePeerConnectionForRemoteClient(
                                    remoteClientHolder = remoteClientHolder,
                                    connectionId = connectionId,
                                    renegotiationDeferred = peerConnectionReadyForNegotiationDeferred
                                )
                                peerConnectionReadyForNegotiationDeferred.await()
                                mapOfPeerConnections[remoteClientHolder] = peerConnectionHolder
                                Timber.d("⚙️ ℹ️ current count of peer connections: ${mapOfPeerConnections.size}")
                            }
                        }

                        is RemoteData -> {
                            if (signalingServerMessage is RemoteData.Offer) {
                                val remoteClientHolder = RemoteClientHolder(id = signalingServerMessage.remoteClientId)
                                Timber.d("⚙️ \uD83D\uDCE1 offer received from remote client: $remoteClientHolder ⬇️")
                                val isSuccess = processOfferFromRemoteClientAndSendAnswer(
                                    offer = signalingServerMessage,
                                    connectionId = connectionId,
                                    remoteClientHolder = remoteClientHolder
                                )
                                if (isSuccess.not()) {
                                    mapOfPeerConnections.remove(remoteClientHolder)
                                    return@collect
                                }
                            }
                            if (signalingServerMessage is RemoteData.IceCandidate) {
                                Timber.d(
                                    "⚙️ \uD83D\uDCE1 ice candidate received from remote client: ${signalingServerMessage.remoteClientId} ⬇️"
                                )
                                addRemoteIceCandidateInWebRtc(signalingServerMessage)
                            }
                        }

                        is Confirmation -> {}
                        is Error -> {
                            Timber.d("⚙️ \uD83D\uDCE1 error ❗ ⬇")
                        }
                    }
                }
        }
        return job
    }

    @Suppress("LongMethod")
    private fun createAndObservePeerConnectionForRemoteClient(
        remoteClientHolder: RemoteClientHolder,
        connectionId: ConnectionIdHolder,
        renegotiationDeferred: CompletableDeferred<Unit>
    ): PeerConnectionHolder {
        val webRtcManager = WebRtcManager(applicationContext)
        val job = webRtcManager.createPeerConnection(remoteClientId = remoteClientHolder.id)
            .onStart { // for debugging
                Timber.d("⚙️ ⚡ start observing webrtc events for remote client: $remoteClientHolder")
            }
            .onCompletion { // for debugging
                Timber.d("⚙️ ⚡ end observing webrtc events for remote client: $remoteClientHolder")
            }
            .onEach { event ->
                when (event) {
                    is PeerConnectionEvent.RenegotiationNeeded -> {
                        Timber.d("⚙️ ⚡ renegotiation needed for remote client: $remoteClientHolder \uD83C\uDD97")
                        renegotiationDeferred.complete(Unit)
                    }

                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("⚙️ ⚡ ice gathering state changed: ${event.state} for remote client: $remoteClientHolder")
                    }

                    is PeerConnectionEvent.IceCandidate -> {
                        Timber.d("⚙️ ⚡ ice candidate generated for remote client: $remoteClientHolder")
                        sendIceCandidateToRemoteClient(connectionId, remoteClientHolder, event.data)
                    }

                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("⚙️ ⚡ signaling state changed: ${event.message} for remote client: $remoteClientHolder")
                    }

                    is PeerConnectionEvent.Connected -> {
                        Timber.d("⚙️ ⚡ peer connection connected for remote client: $remoteClientHolder \uD83D\uDFE2")
                        val dataChannel = DataChannelWrapper(
                            connectionIdHolder = connectionId,
                            webRtcDataChannel = webRtcManager.getDataChannel()
                        )
                        dataChannelObserver.value = dataChannel
                        mapOfDataChannels[connectionId] = DataChannelHolder(
                            connectionIdHolder = connectionId,
                            dataChannel = dataChannel
                        )
                        isAnyChannelConnected.emit(mapOfDataChannels.values.isNotEmpty())
                        Timber.d("⚙️ ℹ️ current count of data channels: ${mapOfDataChannels.size}")
                    }

                    is PeerConnectionEvent.Disconnected -> {
                        Timber.d("⚙️ ⚡ peer connection disconnected for remote client: $remoteClientHolder \uD83D\uDD34")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
                        isAnyChannelConnected.emit(mapOfDataChannels.values.isNotEmpty())
                    }

                    is PeerConnectionEvent.Failed -> {
                        Timber.d("⚙️ ⚡ peer connection failed for remote client: $remoteClientHolder ❌")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ ⚡ an exception occurred: ${exception.localizedMessage} ❗")
            }
            .cancellable()
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)

        return PeerConnectionHolder(
            connectionIdHolder = connectionId,
            webRtcManager = webRtcManager,
            observePeerConnectionJob = job
        )
    }

    private fun terminatePeerConnectionAndDataChannel(
        remoteClientHolder: RemoteClientHolder,
        connectionId: ConnectionIdHolder
    ) {
        Timber.d("⚙️ \uD83D\uDED1 terminating link connection for connectionId: ${connectionId.id}")
        val peerConnectionHolder = mapOfPeerConnections.remove(remoteClientHolder)
        peerConnectionHolder?.let {
            peerConnectionHolder.observePeerConnectionJob.cancel()
            peerConnectionHolder.webRtcManager.close()
        }
        val dataChannelHolder = mapOfDataChannels.remove(connectionId)
        dataChannelHolder?.let {
            dataChannelHolder.dataChannel.close()
        }
        Timber.d("⚙️ \uD83D\uDED1 link connection terminated for connectionId: ${connectionId.id} ✅")
    }

    private suspend fun processOfferFromRemoteClientAndSendAnswer(
        offer: RemoteData.Offer,
        connectionId: ConnectionIdHolder,
        remoteClientHolder: RemoteClientHolder
    ): Boolean {
        suspend fun setRemoteDescriptionFromOffer(offer: RemoteData.Offer): Boolean {
            val sessionDescription = SessionDescriptionWrapper(
                type = SessionDescriptionWrapper.Type.OFFER,
                sessionDescriptionValue = SessionDescriptionWrapper.SessionDescriptionValue(offer.sdp)
            )
            val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
            return webRtcManager.setRemoteDescription(sessionDescription)
                .onFailure {
                    Timber.e(
                        "⚙️ ⚡ failed to set remote description:${it.message} for remote client: $remoteClientHolder❗"
                    )
                }
                .isSuccess
        }

        suspend fun createAndSendAnswerToRemoteClient(
            connectionIdHolder: ConnectionIdHolder,
            remoteClientHolder: RemoteClientHolder
        ): Boolean {
            val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
            val result = webRtcManager.createAnswer()

            result.getOrNull()?.let { sessionDescriptionValue ->
                val localSessionDescription = SessionDescriptionWrapper(
                    type = SessionDescriptionWrapper.Type.ANSWER,
                    sessionDescriptionValue = sessionDescriptionValue
                )
                // first set the local session description
                val isSet = setLocalDescription(
                    remoteClientHolder = remoteClientHolder,
                    localSessionDescription = localSessionDescription
                )
                return if (isSet) {
                    // then send the answer to the remote client via signaling server
                    val webSocketClient = mapOfWebSockets.getValue(connectionIdHolder).webSocketClient
                    Timber.d("⚙️ \uD83D\uDCE1️ send answer to the remote client: $remoteClientHolder ⬆️")
                    webSocketClient.sendAnswerMessage(
                        remoteClientId = remoteClientHolder.id,
                        answerPayload = sessionDescriptionValue.toAnswerPayload()
                    )
                    true
                } else {
                    false
                }
            } ?: kotlin.run {
                Timber.e("⚙️ ⚡ failed to create answer: ${result.exceptionOrNull()?.message}❗")
                return false
            }
        }

        // set remote description of remote client to the wallet's WebRTC
        val remoteDescriptionResult = setRemoteDescriptionFromOffer(offer)
        // create answer with the local description and send it to remote client
        val answerToRemoteClientResult = createAndSendAnswerToRemoteClient(connectionId, remoteClientHolder)
        // both must succeed
        return remoteDescriptionResult && answerToRemoteClientResult
    }

    private suspend fun setLocalDescription(
        remoteClientHolder: RemoteClientHolder,
        localSessionDescription: SessionDescriptionWrapper
    ): Boolean {
        val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
        return webRtcManager.setLocalDescription(localSessionDescription)
            .onFailure {
                Timber.e("⚙️ ⚡ failed to set local description:${it.message} for remote client: $remoteClientHolder❗")
            }
            .isSuccess
    }

    private suspend fun addRemoteIceCandidateInWebRtc(iceCandidate: RemoteData.IceCandidate) {
        val remoteIceCandidate = iceCandidate.remoteIceCandidate
        val remoteClientHolder = RemoteClientHolder(id = iceCandidate.remoteClientId)
        val webRtcManager = mapOfPeerConnections[remoteClientHolder]?.webRtcManager ?: return
        webRtcManager.addRemoteIceCandidate(remoteIceCandidate = remoteIceCandidate)
    }

    private fun sendIceCandidateToRemoteClient(
        connectionId: ConnectionIdHolder,
        remoteClientHolder: RemoteClientHolder,
        iceCandidateData: PeerConnectionEvent.IceCandidate.Data
    ) {
        Timber.d("⚙️ \uD83D\uDCE1️ send ice candidate to the remote client: $remoteClientHolder ⬆️")
        applicationScope.launch(ioDispatcher) {
            val webSocketClient = mapOfWebSockets[connectionId]?.webSocketClient ?: return@launch
            webSocketClient.sendIceCandidateMessage(
                remoteClientId = remoteClientHolder.id,
                iceCandidateData = iceCandidateData
            )
        }
    }
}
