package rdx.works.peerdroid.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.sha256Hash
import rdx.works.core.toHexString
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.wrappers.datachannel.DataChannelEvent
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
import rdx.works.peerdroid.domain.PeerConnectionHolder
import rdx.works.peerdroid.domain.RemoteClientHolder
import rdx.works.peerdroid.domain.WebSocketHolder
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

interface PeerdroidConnector {

    suspend fun connectToConnectorExtension(encryptionKey: ByteArray): Result<Unit>

    suspend fun deleteConnector(connectionIdHolder: ConnectionIdHolder)

    suspend fun terminateConnectionToConnectorExtension()

    val dataChannelMessagesFromRemoteClients: Flow<DataChannelEvent>

    suspend fun sendDataChannelMessageToRemoteClient(remoteClientId: String, message: String): Result<Unit>
}

internal class PeerdroidConnectorImpl(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PeerdroidConnector {

    // one per CE. One CE can have multiple remote clients
    private val mapOfWebSockets = ConcurrentHashMap<ConnectionIdHolder, WebSocketHolder>()

    // one per remote client (dapp)
    private val mapOfPeerConnections = ConcurrentHashMap<RemoteClientHolder, PeerConnectionHolder>()

    // one per remote client (dapp)
    private val mapOfDataChannels = ConcurrentHashMap<RemoteClientHolder, DataChannelHolder>()

    // every time a new data channel opens it will be added in the dataChannelMessagesFromRemoteClients
    private val dataChannelObserver = MutableStateFlow<DataChannelWrapper?>(null)

    init {
        Timber.d("⚙️ init PeerdroidConnector")
    }

    override suspend fun connectToConnectorExtension(encryptionKey: ByteArray): Result<Unit> {
        val connectionId = encryptionKey.sha256Hash().toHexString()

        return withContext(ioDispatcher) {
            if (mapOfWebSockets.containsKey(ConnectionIdHolder(id = connectionId))) {
                Timber.d("⚙️ already connected to CE with connectionId: $connectionId")
                return@withContext Result.Error(
                    message = "Link connection is already established for the connectionId: $connectionId"
                )
            }

            Timber.d("⚙️ connect to CE with connectionId: $connectionId")
            val webSocketClient = WebSocketClient(applicationContext)
            val result = webSocketClient.initSession(
                connectionId = connectionId,
                encryptionKey = encryptionKey
            )
            when (result) {
                is Result.Success -> {
                    val connectionHolder = ConnectionIdHolder(id = connectionId)
                    val job = listenForMessagesFromRemoteClients(connectionHolder, webSocketClient)
                    mapOfWebSockets[connectionHolder] = WebSocketHolder(
                        webSocketClient = webSocketClient,
                        listenMessagesJob = job
                    )
                    result
                }
                is Result.Error -> {
                    Timber.e("⚙️ failed to connect to CE with connectionId: $connectionId")
                    result
                }
            }
        }
    }

    override suspend fun deleteConnector(connectionIdHolder: ConnectionIdHolder) {
        Timber.d("⚙️ delete connector with connectionId: $connectionIdHolder")
        withContext(ioDispatcher) {
            val webSocketHolder = mapOfWebSockets.remove(connectionIdHolder)
            webSocketHolder?.let {
                webSocketHolder.listenMessagesJob.cancel()
                webSocketHolder.webSocketClient.closeSession()
            }
            // close and remove the peer connections and their jobs for this connection id
            val peerConnectionsForTermination = mapOfPeerConnections.values.filter { peerConnectionHolder ->
                peerConnectionHolder.connectionIdHolder == connectionIdHolder
            }
            peerConnectionsForTermination.forEach { peerConnectionHolder ->
                peerConnectionHolder.observePeerConnectionJob.cancel()
                peerConnectionHolder.webRtcManager.close()
            }
            mapOfPeerConnections.values.removeAll(peerConnectionsForTermination.toSet())
            // close and remove data channels for this connection id
            val dataChannelsForTermination = mapOfDataChannels.values.filter { dataChannelHolder ->
                dataChannelHolder.connectionIdHolder == connectionIdHolder
            }
            dataChannelsForTermination.forEach { dataChannelHolder ->
                dataChannelHolder.dataChannel.close()
            }
            mapOfDataChannels.values.removeAll(dataChannelsForTermination.toSet())
        }
    }

    override suspend fun terminateConnectionToConnectorExtension() {
        Timber.d("⚙️ terminate connection to connector extension")
        withContext(ioDispatcher) {
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
        }
    }

    // here we merge all the incoming messages
    // from the opened data channels into a single flow
    @OptIn(FlowPreview::class)
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
    ).flowOn(ioDispatcher)

    override suspend fun sendDataChannelMessageToRemoteClient(
        remoteClientId: String,
        message: String
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            val remoteClientHolder = RemoteClientHolder(id = remoteClientId)
            mapOfDataChannels.getValue(remoteClientHolder).dataChannel.sendMessage(message)
        }
    }

    private fun listenForMessagesFromRemoteClients(
        connectionIdHolder: ConnectionIdHolder,
        webSocketClient: WebSocketClient
    ): Job {
        val job = applicationScope.launch(ioDispatcher) {
            webSocketClient
                .listenForMessages()
                .collect { signalingServerMessage ->
                    when (signalingServerMessage) {
                        is RemoteInfo -> {
                            if (signalingServerMessage is RemoteInfo.ClientConnected) {
                                Timber.d("⚙️ ⬇️  remote client connected with id: ${signalingServerMessage.remoteClientId}")
                                val peerConnectionReadyForNegotiationDeferred = CompletableDeferred<Unit>()
                                val remoteClientHolder = RemoteClientHolder(id = signalingServerMessage.remoteClientId)
                                val peerConnectionHolder = createAndObservePeerConnectionForRemoteClient(
                                    remoteClientHolder = remoteClientHolder,
                                    connectionIdHolder = connectionIdHolder,
                                    renegotiationDeferred = peerConnectionReadyForNegotiationDeferred
                                )
                                peerConnectionReadyForNegotiationDeferred.await()
                                mapOfPeerConnections[remoteClientHolder] = peerConnectionHolder
                                Timber.d("⚙️ count of peer connections: ${mapOfPeerConnections.size}")
                            }
                        }
                        is RemoteData -> {
                            if (signalingServerMessage is RemoteData.Offer) {
                                val remoteClientHolder = RemoteClientHolder(id = signalingServerMessage.remoteClientId)
                                Timber.d("⚙️ ⬇️  offer received from remote client: $remoteClientHolder")
                                val isSuccess = processOfferFromRemoteClientAndSendAnswer(
                                    offer = signalingServerMessage,
                                    connectionIdHolder = connectionIdHolder,
                                    remoteClientHolder = remoteClientHolder
                                )
                                if (isSuccess.not()) {
                                    mapOfPeerConnections.remove(remoteClientHolder)
                                    return@collect
                                }
                            }
                            if (signalingServerMessage is RemoteData.IceCandidate) {
                                val remoteClientHolder = RemoteClientHolder(id = signalingServerMessage.remoteClientId)
                                Timber.d("⚙️ ⬇️ ice candidate received from remote client: $remoteClientHolder")
                                addRemoteIceCandidateInWebRtc(signalingServerMessage)
                            }
                        }
                        is Confirmation -> {} // TODO do something with this poor but important event
                        is Error -> {
                            Timber.d("⚙️ ⬇️ error")
                        }
                    }
                }
        }
        return job
    }

    @Suppress("LongMethod")
    private fun createAndObservePeerConnectionForRemoteClient(
        remoteClientHolder: RemoteClientHolder,
        connectionIdHolder: ConnectionIdHolder,
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
                        Timber.d("⚙️ ⚡ renegotiation needed 🆗 for remote client: $remoteClientHolder")
                        renegotiationDeferred.complete(Unit)
                    }
                    is PeerConnectionEvent.IceGatheringChange -> {
                        Timber.d("⚙️ ⚡ ice gathering state changed: ${event.state} for remote client: $remoteClientHolder")
                    }
                    is PeerConnectionEvent.IceCandidate -> {
                        Timber.d("⚙️ ⚡ ice candidate generated for remote client: $remoteClientHolder")
                        sendIceCandidateToRemoteClient(connectionIdHolder, remoteClientHolder, event.data)
                    }
                    is PeerConnectionEvent.SignalingState -> {
                        Timber.d("⚙️ ⚡ signaling state changed: ${event.message} for remote client: $remoteClientHolder")
                    }
                    is PeerConnectionEvent.Connected -> {
                        Timber.d("⚙️ ⚡ peer connection connected 🟢 for remote client: $remoteClientHolder")
                        val dataChannel = DataChannelWrapper(
                            remoteClientId = remoteClientHolder.id,
                            webRtcDataChannel = webRtcManager.getDataChannel()
                        )
                        dataChannelObserver.value = dataChannel
                        mapOfDataChannels[remoteClientHolder] = DataChannelHolder(
                            connectionIdHolder = connectionIdHolder,
                            dataChannel = dataChannel
                        )
                        Timber.d("⚙️ count of data channels: ${mapOfDataChannels.size}")
                    }
                    is PeerConnectionEvent.Disconnected -> {
                        Timber.d("⚙️ ⚡ peer connection disconnected 🔴 for remote client: $remoteClientHolder")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder)
                    }
                    is PeerConnectionEvent.Failed -> {
                        Timber.d("⚙️ ⚡ peer connection failed ❌ for remote client: $remoteClientHolder")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder)
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ ⚡ an exception occurred: ${exception.localizedMessage}")
            }
            .cancellable()
            .flowOn(ioDispatcher)
            .launchIn(applicationScope)

        return PeerConnectionHolder(
            connectionIdHolder = connectionIdHolder,
            webRtcManager = webRtcManager,
            observePeerConnectionJob = job
        )
    }

    private fun terminatePeerConnectionAndDataChannel(remoteClientHolder: RemoteClientHolder) {
        val peerConnectionHolder = mapOfPeerConnections.remove(remoteClientHolder)
        peerConnectionHolder?.let {
            peerConnectionHolder.observePeerConnectionJob.cancel()
            peerConnectionHolder.webRtcManager.close()
        }
        val dataChannelHolder = mapOfDataChannels.remove(remoteClientHolder)
        dataChannelHolder?.let {
            dataChannelHolder.dataChannel.close()
        }
    }

    private suspend fun processOfferFromRemoteClientAndSendAnswer(
        offer: RemoteData.Offer,
        connectionIdHolder: ConnectionIdHolder,
        remoteClientHolder: RemoteClientHolder
    ): Boolean {
        suspend fun setRemoteDescriptionFromOffer(offer: RemoteData.Offer): Boolean {
            val sessionDescription = SessionDescriptionWrapper(
                type = SessionDescriptionWrapper.Type.OFFER,
                sessionDescriptionValue = SessionDescriptionWrapper.SessionDescriptionValue(offer.sdp)
            )
            val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
            return when (val result = webRtcManager.setRemoteDescription(sessionDescription)) {
                is Result.Success -> {
                    Timber.d("⚙️ ⚡ remote description is set for remote client: $remoteClientHolder")
                    true
                }
                is Result.Error -> {
                    Timber.e("⚙️ ⚡ failed to set remote description:${result.message} for remote client: $remoteClientHolder")
                    false
                }
            }
        }

        suspend fun createAndSendAnswerToRemoteClient(
            connectionIdHolder: ConnectionIdHolder,
            remoteClientHolder: RemoteClientHolder
        ): Boolean {
            val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
            when (val result = webRtcManager.createAnswer()) {
                is Result.Success -> {
                    val sessionDescriptionValue = result.data // TODO map this thing here to the wrapper
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
                        Timber.d("⚙️ ⬆️ send answer to the remote client: $remoteClientHolder")
                        webSocketClient.sendAnswerMessage(
                            remoteClientId = remoteClientHolder.id,
                            answerPayload = sessionDescriptionValue.toAnswerPayload()
                        )
                        true
                    } else {
                        false
                    }
                }
                is Result.Error -> {
                    Timber.e("⚙️ ⚡ failed to create answer: ${result.message}")
                    return false
                }
            }
        }

        val remoteDescriptionResult = setRemoteDescriptionFromOffer(offer)
        val answerToRemoteClientResult = createAndSendAnswerToRemoteClient(connectionIdHolder, remoteClientHolder)
        return remoteDescriptionResult && answerToRemoteClientResult
    }

    private suspend fun setLocalDescription(
        remoteClientHolder: RemoteClientHolder,
        localSessionDescription: SessionDescriptionWrapper
    ): Boolean {
        val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
        return when (val result = webRtcManager.setLocalDescription(localSessionDescription)) {
            is Result.Success -> {
                Timber.d("⚙️ ⚡ local description is set for remote client: $remoteClientHolder")
                true
            }
            is Result.Error -> {
                Timber.e("⚙️ ⚡ failed to set local description:${result.message} for remote client: $remoteClientHolder")
                false
            }
        }
    }

    private suspend fun addRemoteIceCandidateInWebRtc(iceCandidate: RemoteData.IceCandidate) {
        val remoteIceCandidate = iceCandidate.remoteIceCandidate
        val remoteClientHolder = RemoteClientHolder(id = iceCandidate.remoteClientId)
        Timber.d("⚙️ ⚡ set remote ice candidate in local WebRTC for remote client: $remoteClientHolder")
        val webRtcManager = mapOfPeerConnections.getValue(remoteClientHolder).webRtcManager
        webRtcManager.addRemoteIceCandidate(remoteIceCandidate = remoteIceCandidate)
    }

    private fun sendIceCandidateToRemoteClient(
        connectionIdHolder: ConnectionIdHolder,
        remoteClientHolder: RemoteClientHolder,
        iceCandidateData: PeerConnectionEvent.IceCandidate.Data
    ) {
        Timber.d("⚙️ ⬆️ send ice candidate to the remote client: $remoteClientHolder")
        applicationScope.launch(ioDispatcher) {
            val webSocketClient = mapOfWebSockets.getValue(connectionIdHolder).webSocketClient
            webSocketClient.sendIceCandidateMessage(
                remoteClientId = remoteClientHolder.id,
                iceCandidateData = iceCandidateData
            )
        }
    }
}
