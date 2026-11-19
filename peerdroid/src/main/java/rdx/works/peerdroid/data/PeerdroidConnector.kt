@file:Suppress("TooManyFunctions")

package rdx.works.peerdroid.data

import android.content.Context
import android.os.SystemClock
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rdx.works.peerdroid.data.webrtc.DataChannelReaders
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
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionHolder
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.RemoteClientHolder
import rdx.works.peerdroid.domain.WebSocketHolder
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

interface PeerdroidConnector {

    val anyChannelConnected: Flow<Boolean>

    val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>>

    suspend fun connectToConnectorExtension(
        p2pTransportProfile: P2pTransportProfile,
        encryptionKey: RadixConnectPassword
    ): Result<Unit>

    suspend fun deleteConnector(connectionId: ConnectionIdHolder)

    fun terminateConnectionToConnectorExtension()

    val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent>

    suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit>

    suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit>

    /**
     * The app came back to the foreground: the connections that may have died while it was in the background are
     * checked, and reopened when they are down. Returns at once.
     */
    fun onAppForeground() = Unit

    /**
     * The app went to the background. Returns at once.
     */
    fun onAppBackground() = Unit
}

internal class PeerdroidConnectorImpl(
    @ApplicationContext private val applicationContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    // The clients of the signaling server and of WebRTC, and a monotonic clock in ms that counts deep sleep too (a
    // negotiation frozen in the background has lasted that long): replaced in tests.
    private val webSocketClientFactory: () -> WebSocketClient = { WebSocketClient(applicationContext) },
    private val webRtcManagerFactory: (P2pTransportProfile) -> WebRtcManager = { WebRtcManager(applicationContext, it) },
    private val monotonicMillis: () -> Long = { SystemClock.elapsedRealtime() }
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

    // Every open data channel's reader, all handing their messages to dataChannelMessagesFromRemoteClients.
    private val readers = DataChannelReaders(applicationScope + ioDispatcher)

    private val isAnyChannelConnected = MutableStateFlow(false)

    private val _peerConnectionStatus = MutableStateFlow(emptyMap<String, PeerConnectionStatus>())

    // Serialises, link by link, starting, stopping and restarting the signaling session, so that none undoes another.
    private val linkLocks = ConcurrentHashMap<ConnectionIdHolder, Mutex>()

    // Bumped by terminateConnectionToConnectorExtension: a restart begun before it must not bring a link back.
    private val generation = AtomicLong(0)

    // The links whose signaling session could not be opened when they were asked for (e.g. no network when the app
    // started): opened again in the foreground, as the links whose session died are.
    private val failedStarts = ConcurrentHashMap<ConnectionIdHolder, FailedStart>()

    // Not a data class: it holds the password, which must never end up in a log through toString().
    private class FailedStart(val p2pTransportProfile: P2pTransportProfile, val password: RadixConnectPassword)

    // Since when (monotonic ms) each link negotiating its peer connection has been doing so.
    private val connectingSinceMs = ConcurrentHashMap<ConnectionIdHolder, Long>()

    init {
        Timber.d("⚙️ init PeerdroidConnector")
    }

    override val anyChannelConnected: Flow<Boolean>
        get() = isAnyChannelConnected.asSharedFlow()

    override val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>>
        get() = _peerConnectionStatus

    override suspend fun connectToConnectorExtension(
        p2pTransportProfile: P2pTransportProfile,
        encryptionKey: RadixConnectPassword
    ): Result<Unit> {
        val connectionId = ConnectionIdHolder(encryptionKey)

        return withContext(ioDispatcher) {
            val atGeneration = generation.get()
            lockOf(connectionId).withLock {
                val current = mapOfWebSockets[connectionId]
                if (current?.p2pTransportProfile == p2pTransportProfile) {
                    Timber.d("⚙️ already tried to establish a link connection with connectionId: $connectionId")
                    return@withLock Result.success(Unit)
                }
                // Another signaling server: the session with the previous one is closed, not left open.
                current?.let { stopSignaling(connectionId, it) }
                startSignaling(connectionId, p2pTransportProfile, encryptionKey, reason = "started", atGeneration)
            }
        }
    }

    private fun lockOf(connectionId: ConnectionIdHolder): Mutex = linkLocks.computeIfAbsent(connectionId) { Mutex() }

    // Requires the link's lock.
    private suspend fun stopSignaling(connectionId: ConnectionIdHolder, holder: WebSocketHolder) {
        mapOfWebSockets.remove(connectionId, holder)
        holder.listenMessagesJob.cancel()
        holder.webSocketClient.closeSession()
    }

    // Requires the link's lock. Opens the link's session with the signaling server and listens to it, unless every
    // connection was terminated since [atGeneration] (the session is then closed again, not kept).
    private suspend fun startSignaling(
        connectionId: ConnectionIdHolder,
        p2pTransportProfile: P2pTransportProfile,
        password: RadixConnectPassword,
        reason: String,
        atGeneration: Long
    ): Result<Unit> {
        Timber.d("⚙️ establishing a link connection with connectionId: $connectionId")
        val webSocketClient = webSocketClientFactory()
        return webSocketClient.initSession(
            connectionId = connectionId,
            encryptionKey = password,
            signalingServerUrl = p2pTransportProfile.signalingServer
        ).onSuccess {
            failedStarts.remove(connectionId)
            if (generation.get() != atGeneration) {
                webSocketClient.closeSession()
                Timber.d("⚙️ link connection with connectionId: $connectionId not kept: every connection was terminated meanwhile")
                return Result.success(Unit)
            }
            val job = listenForMessagesFromRemoteClients(p2pTransportProfile, connectionId, webSocketClient)
            mapOfWebSockets[connectionId] = WebSocketHolder(
                webSocketClient = webSocketClient,
                listenMessagesJob = job,
                p2pTransportProfile = p2pTransportProfile,
                password = password
            )
            Timber.d("⚙️ link connection with connectionId: $connectionId $reason ✅")
        }.onFailure {
            if (generation.get() == atGeneration) {
                failedStarts[connectionId] = FailedStart(p2pTransportProfile = p2pTransportProfile, password = password)
            }
            Timber.e("⚙️ failed to establish a link connection with connectionId: $connectionId")
        }
    }

    override suspend fun deleteConnector(connectionId: ConnectionIdHolder) {
        Timber.d("⚙️ \uD83D\uDDD1️ delete link connection with connectionId: ${connectionId.id}")
        withContext(ioDispatcher) {
            lockOf(connectionId).withLock { deleteLocked(connectionId) }
        }
    }

    // Requires the link's lock.
    private suspend fun deleteLocked(connectionId: ConnectionIdHolder) {
        failedStarts.remove(connectionId)
        mapOfWebSockets[connectionId]?.let { holder -> stopSignaling(connectionId, holder) }
        // close and remove data channels for this connection id
        val dataChannelsForTermination = mapOfDataChannels.values.filter { dataChannelHolder ->
            dataChannelHolder.connectionIdHolder == connectionId
        }
        dataChannelsForTermination.forEach { dataChannelHolder ->
            dataChannelHolder.dataChannel.close()
        }
        // close and remove the peer connections, their jobs and their readers for this connection id
        val peerConnectionsForTermination = mapOfPeerConnections.filterValues { peerConnectionHolder ->
            peerConnectionHolder.connectionIdHolder == connectionId
        }
        peerConnectionsForTermination.forEach { (remoteClientHolder, peerConnectionHolder) ->
            readers.stop(remoteClientHolder)
            peerConnectionHolder.observePeerConnectionJob.cancel()
            peerConnectionHolder.webRtcManager.close()
        }
        mapOfPeerConnections.keys.removeAll(peerConnectionsForTermination.keys)
        mapOfDataChannels.values.removeAll(dataChannelsForTermination.toSet())
        updatePeerConnectionStatus(connectionId = connectionId, isOpen = false, isDeleted = true)
        Timber.d("⚙️ \uD83D\uDDD1️ link connection with connectionId: ${connectionId.id} deleted ✅")
    }

    override fun terminateConnectionToConnectorExtension() {
        generation.incrementAndGet()
        failedStarts.clear()
        connectingSinceMs.clear()
        applicationScope.launch(ioDispatcher) {
            Timber.d("⚙️ \uD83D\uDEAB terminating all link connections and clear: websockets, peer connections, and data channels")
            mapOfWebSockets.values.forEach { webSocketHolder ->
                webSocketHolder.listenMessagesJob.cancel()
                webSocketHolder.webSocketClient.closeSession()
            }
            mapOfWebSockets.clear()

            readers.stopAll()
            mapOfDataChannels.values.forEach { dataChannelHolder ->
                dataChannelHolder.dataChannel.close()
            }
            mapOfDataChannels.clear()

            mapOfPeerConnections.values.forEach { peerConnectionHolder ->
                peerConnectionHolder.observePeerConnectionJob.cancel()
                peerConnectionHolder.webRtcManager.close()
            }
            mapOfPeerConnections.clear()
            _peerConnectionStatus.tryEmit(emptyMap())
            Timber.d("⚙️ \uD83D\uDEAB all link connection terminated and cleared ✅")
        }
    }

    // The incoming messages of every open data channel, in one flow (see DataChannelReaders).
    override val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent> = readers.messages

    override suspend fun sendDataChannelMessageToRemoteClient(
        remoteConnectorId: String,
        message: String
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            val connectionIdHolder = ConnectionIdHolder(id = remoteConnectorId)
            if (mapOfDataChannels.contains(key = connectionIdHolder)) {
                mapOfDataChannels[connectionIdHolder]
                    ?.dataChannel
                    ?.sendMessage(message)
                    ?: Result.failure(MessageNotSentException("failed to send message to CE because data channel doesn't exist"))
            } else {
                Timber.e("📯 failed to send message to CE: $connectionIdHolder because its data channel is closed❗")
                Result.failure(MessageNotSentException("failed to send message to CE"))
            }
        }
    }

    // Whether the app went to the background since it was last in the foreground.
    @Volatile
    private var wentToBackground = false

    override fun onAppBackground() {
        wentToBackground = true
    }

    // The signaling session of a link may have died while the app was in the background (nothing notices: the socket has
    // no keep-alive), and then the connector extension cannot reach the wallet again. So back in the foreground, every
    // link that is neither open nor negotiating opens a new signaling session, link by link and all at once; the links
    // connected are left alone. A link whose session could not be opened at all (e.g. no network when the app started)
    // is opened then too, and so is a link whose negotiation has not ended after NEGOTIATION_LIMIT_MS (e.g. frozen in
    // the background).
    override fun onAppForeground() {
        val wasInBackground = wentToBackground
        wentToBackground = false
        if (!wasInBackground) return
        val atGeneration = generation.get()
        applicationScope.launch(ioDispatcher) {
            (mapOfWebSockets.keys + failedStarts.keys).toSet().forEach { connectionId ->
                launch { restartSignalingIfClosed(connectionId, atGeneration) }
            }
        }
    }

    private suspend fun restartSignalingIfClosed(connectionId: ConnectionIdHolder, atGeneration: Long) {
        lockOf(connectionId).withLock {
            if (generation.get() != atGeneration) return@withLock
            val holder = mapOfWebSockets[connectionId]
            if (holder == null) {
                val failedStart = failedStarts[connectionId] ?: return@withLock
                startSignaling(
                    connectionId = connectionId,
                    p2pTransportProfile = failedStart.p2pTransportProfile,
                    password = failedStart.password,
                    reason = "started (app foreground, after a failed start)",
                    atGeneration = atGeneration
                )
                return@withLock
            }
            when (_peerConnectionStatus.value[connectionId.id]) {
                PeerConnectionStatus.OPEN -> return@withLock
                PeerConnectionStatus.CONNECTING -> {
                    val since = connectingSinceMs[connectionId]
                    if (since != null && monotonicMillis() - since < NEGOTIATION_LIMIT_MS) return@withLock
                    // A negotiation that has not ended by now never will: its peer connection goes with the session.
                    closePeerConnections(connectionId)
                }
                else -> Unit
            }
            stopSignaling(connectionId, holder)
            startSignaling(
                connectionId = connectionId,
                p2pTransportProfile = holder.p2pTransportProfile,
                password = holder.password,
                reason = "restarted (app foreground, no live channel)",
                atGeneration = atGeneration
            )
        }
    }

    // Requires the link's lock.
    private fun closePeerConnections(connectionId: ConnectionIdHolder) {
        mapOfPeerConnections.filterValues { it.connectionIdHolder == connectionId }.keys
            .forEach { remoteClientHolder -> terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId) }
        isAnyChannelConnected.tryEmit(mapOfDataChannels.values.isNotEmpty())
        updatePeerConnectionStatus(connectionId = connectionId, isConnecting = false, isOpen = false)
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
        p2pTransportProfile: P2pTransportProfile,
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
                                    p2pTransportProfile = p2pTransportProfile,
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
                                    terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
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
        p2pTransportProfile: P2pTransportProfile,
        remoteClientHolder: RemoteClientHolder,
        connectionId: ConnectionIdHolder,
        renegotiationDeferred: CompletableDeferred<Unit>
    ): PeerConnectionHolder {
        val webRtcManager = webRtcManagerFactory(p2pTransportProfile)
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
                        updatePeerConnectionStatus(connectionId = connectionId, isConnecting = true)
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
                        readers.start(remoteClient = remoteClientHolder, events = dataChannel.dataChannelEvents)
                        mapOfDataChannels[connectionId] = DataChannelHolder(
                            connectionIdHolder = connectionId,
                            remoteClientHolder = remoteClientHolder,
                            dataChannel = dataChannel
                        )
                        isAnyChannelConnected.tryEmit(mapOfDataChannels.values.isNotEmpty())
                        updatePeerConnectionStatus(connectionId = connectionId, isConnecting = false, isOpen = true)
                        Timber.d("⚙️ ℹ️ current count of data channels: ${mapOfDataChannels.size}")
                    }

                    is PeerConnectionEvent.Disconnected -> {
                        Timber.d("⚙️ ⚡ peer connection disconnected for remote client: $remoteClientHolder \uD83D\uDD34")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
                        isAnyChannelConnected.tryEmit(mapOfDataChannels.values.isNotEmpty())
                        updatePeerConnectionStatus(connectionId = connectionId, isConnecting = false, isOpen = false)
                    }

                    is PeerConnectionEvent.Failed -> {
                        Timber.d("⚙️ ⚡ peer connection failed for remote client: $remoteClientHolder ❌")
                        terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
                        isAnyChannelConnected.tryEmit(mapOfDataChannels.values.isNotEmpty())
                        updatePeerConnectionStatus(connectionId = connectionId, isConnecting = false, isOpen = false)
                    }
                }
            }
            .catch { exception ->
                Timber.e("⚙️ ⚡ an exception occurred: ${exception.localizedMessage} ❗")
                terminatePeerConnectionAndDataChannel(remoteClientHolder, connectionId)
                isAnyChannelConnected.tryEmit(mapOfDataChannels.values.isNotEmpty())
                updatePeerConnectionStatus(connectionId = connectionId, isConnecting = false, isOpen = false)
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
        Timber.d("⚙️ \uD83D\uDEAB terminating link connection for remote client: $remoteClientHolder")
        readers.stop(remoteClientHolder)
        // The link's data channel, only if it is this connection's: a connection that went away after the link's
        // next one opened must not take the next one's channel with it (its own closes with its peer connection).
        val dataChannelHolder = mapOfDataChannels[connectionId]?.takeIf { it.remoteClientHolder == remoteClientHolder }
        dataChannelHolder?.let {
            mapOfDataChannels.remove(connectionId, dataChannelHolder)
            dataChannelHolder.dataChannel.close()
        }
        val peerConnectionHolder = mapOfPeerConnections.remove(remoteClientHolder)
        peerConnectionHolder?.let {
            peerConnectionHolder.observePeerConnectionJob.cancel()
            peerConnectionHolder.webRtcManager.close()
        }
        Timber.d("⚙️ \uD83D\uDEAB link connection terminated for remote client: $remoteClientHolder ✅")
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
            val webRtcManager = mapOfPeerConnections[remoteClientHolder]?.webRtcManager ?: return false
            return webRtcManager.setRemoteDescription(sessionDescription)
                .onFailure {
                    Timber.e(
                        "⚙️ ⚡ failed to set remote description:${it.message} for remote client: $remoteClientHolder❗"
                    )
                }
                .isSuccess
        }

        @Suppress("ReturnCount")
        suspend fun createAndSendAnswerToRemoteClient(
            connectionIdHolder: ConnectionIdHolder,
            remoteClientHolder: RemoteClientHolder
        ): Boolean {
            val webRtcManager = mapOfPeerConnections[remoteClientHolder]?.webRtcManager ?: return false
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
                    val webSocketClient = mapOfWebSockets[connectionIdHolder]?.webSocketClient ?: return false
//                    Timber.d("⚙️ \uD83D\uDCE1️ send answer to the remote client: $remoteClientHolder ⬆️")
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
        val webRtcManager = mapOfPeerConnections[remoteClientHolder]?.webRtcManager ?: return false
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
//        Timber.d("⚙️ \uD83D\uDCE1️ send ice candidate to the remote client: $remoteClientHolder ⬆️")
        applicationScope.launch(ioDispatcher) {
            val webSocketClient = mapOfWebSockets[connectionId]?.webSocketClient ?: return@launch
            webSocketClient.sendIceCandidateMessage(
                remoteClientId = remoteClientHolder.id,
                iceCandidateData = iceCandidateData
            )
        }
    }

    private fun updatePeerConnectionStatus(
        connectionId: ConnectionIdHolder,
        isConnecting: Boolean = false,
        isOpen: Boolean = false,
        isDeleted: Boolean = false
    ) {
        val status = when {
            isDeleted -> null
            isConnecting -> PeerConnectionStatus.CONNECTING
            isOpen -> PeerConnectionStatus.OPEN
            else -> PeerConnectionStatus.CLOSED
        }
        // Atomically: links change at the same time (e.g. restarted all at once), and none may undo another's change.
        var previousStatus: PeerConnectionStatus? = null
        _peerConnectionStatus.update { statuses ->
            previousStatus = statuses[connectionId.id]
            if (status == null) statuses - connectionId.id else statuses + (connectionId.id to status)
        }
        when {
            status != PeerConnectionStatus.CONNECTING -> connectingSinceMs.remove(connectionId)
            previousStatus != PeerConnectionStatus.CONNECTING -> connectingSinceMs[connectionId] = monotonicMillis()
        }
    }
}

/** How long a peer connection may negotiate before the foreground takes it for stuck and opens the link again. */
private const val NEGOTIATION_LIMIT_MS = 30_000L
