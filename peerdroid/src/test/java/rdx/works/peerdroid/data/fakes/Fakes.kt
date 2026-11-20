package rdx.works.peerdroid.data.fakes

import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.P2pStunServer
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.P2pTurnServer
import com.radixdlt.sargon.RadixConnectPassword
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.toBagOfBytes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import rdx.works.core.domain.RadixConnectTransport
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.data.iroh.IrohConnectionStatus
import rdx.works.peerdroid.data.iroh.IrohConnector
import rdx.works.peerdroid.data.iroh.IrohEvent
import rdx.works.peerdroid.data.iroh.IrohSessionFactory
import rdx.works.peerdroid.data.iroh.IrohSessionHandle
import rdx.works.peerdroid.data.iroh.NetworkChangeMonitor
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.IrohPath
import rdx.works.peerdroid.domain.IrohSessionDiagnostics
import rdx.works.peerdroid.domain.LinkEvent
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics

fun password(seed: Int): RadixConnectPassword = RadixConnectPassword(
    value = Exactly32Bytes.init(ByteArray(32) { index -> (seed + index).toByte() }.toBagOfBytes())
)

val transportProfile = P2pTransportProfile(
    name = "Test",
    signalingServer = "wss://signaling.example.com",
    stun = P2pStunServer(urls = listOf("stun:stun.example.com:19302")),
    turn = P2pTurnServer(urls = emptyList(), username = null, credential = null)
)

class FakeTransportSettings(
    transport: RadixConnectTransport = RadixConnectTransport.DEFAULT,
    relayUrl: String? = null,
    irohCapableConnectionIds: Set<String> = emptySet()
) : RadixConnectTransportSettings {

    val transportState = MutableStateFlow(transport)
    val relayUrlState = MutableStateFlow(relayUrl)
    val irohCapableState = MutableStateFlow(irohCapableConnectionIds)

    val remembered = mutableListOf<String>()
    val forgotten = mutableListOf<String>()

    // The iroh link memory kept per link (connection id).
    val linkMemories = mutableMapOf<String, String>()

    override val transport: Flow<RadixConnectTransport> = transportState
    override val irohRelayUrl: Flow<String?> = relayUrlState
    override val irohCapableConnectionIds: Flow<Set<String>> = irohCapableState

    override suspend fun rememberIrohSupport(connectionId: ConnectionIdHolder) {
        remembered += connectionId.id
        irohCapableState.value = irohCapableState.value + connectionId.id
    }

    override suspend fun irohLinkMemory(connectionId: ConnectionIdHolder): String? = linkMemories[connectionId.id]

    override suspend fun rememberIrohLinkMemory(connectionId: ConnectionIdHolder, memory: String) {
        linkMemories[connectionId.id] = memory
    }

    override suspend fun forgetLink(connectionId: ConnectionIdHolder) {
        forgotten += connectionId.id
        irohCapableState.value = irohCapableState.value - connectionId.id
        linkMemories.remove(connectionId.id)
    }
}

/**
 * A transport whose connections and send results are scripted by the test.
 */
class FakePeerdroidConnector : PeerdroidConnector {

    val statuses = MutableStateFlow(emptyMap<String, PeerConnectionStatus>())
    val connected = MutableStateFlow(false)
    val messages = MutableSharedFlow<DataChannelWrapperEvent>()

    val connectedIds = mutableListOf<String>()
    val deletedIds = mutableListOf<String>()
    val sentMessages = mutableListOf<Pair<String, String>>()
    var terminateCount = 0
    var connectResult: Result<Unit> = Result.success(Unit)

    // When set, connecting waits for it (e.g. a slow signaling server).
    var connectGate: CompletableDeferred<Unit>? = null

    /**
     * The result of sending to a connector. By default a message is delivered only if the connector is open.
     */
    var sendResult: (connectorId: String) -> Result<Unit> = { connectorId ->
        if (statuses.value[connectorId] == PeerConnectionStatus.OPEN) {
            Result.success(Unit)
        } else {
            Result.failure(MessageNotSentException("not connected"))
        }
    }

    fun setStatus(connectorId: String, status: PeerConnectionStatus) {
        statuses.value = statuses.value + (connectorId to status)
    }

    override val anyChannelConnected: Flow<Boolean> = connected

    override val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>> = statuses

    override val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent> = messages

    override suspend fun connectToConnectorExtension(
        p2pTransportProfile: P2pTransportProfile,
        encryptionKey: RadixConnectPassword
    ): Result<Unit> {
        connectGate?.await()
        connectedIds += ConnectionIdHolder(encryptionKey).id
        return connectResult
    }

    override suspend fun deleteConnector(connectionId: ConnectionIdHolder) {
        deletedIds += connectionId.id
        connectedIds.remove(connectionId.id)
    }

    override fun terminateConnectionToConnectorExtension() {
        terminateCount++
        connectedIds.clear()
    }

    override suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit> =
        sendResult(remoteConnectorId).onSuccess { sentMessages += remoteConnectorId to message }

    override suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit> {
        val results = statuses.value.keys.map { connectorId -> sendDataChannelMessageToRemoteClient(connectorId, message) }
        return results.firstOrNull { it.isSuccess } ?: Result.failure(MessageNotSentException("none connected"))
    }

    var diagnosticsOf: (ConnectionIdHolder) -> TransportDiagnostics? = { null }

    override fun diagnostics(connectionId: ConnectionIdHolder): TransportDiagnostics? = diagnosticsOf(connectionId)

    var recentEventsOf: (ConnectionIdHolder) -> List<LinkEvent> = { emptyList() }

    var appForegroundCount = 0

    override fun onAppForeground() {
        appForegroundCount++
    }

    var appBackgroundCount = 0

    override fun onAppBackground() {
        appBackgroundCount++
    }

    val inFrontToldValues = mutableListOf<Boolean>()

    override fun onAppInFront(inFront: Boolean) {
        inFrontToldValues += inFront
    }

    override fun recentEvents(connectionId: ConnectionIdHolder): List<LinkEvent> = recentEventsOf(connectionId)
}

internal class FakeIrohSession(
    val connectionId: ConnectionIdHolder,
    val relayUrl: String?,
    // The link memory the session was started with.
    val startedWithMemory: String? = null
) : IrohSessionHandle {

    // What the session reports it remembers of its link.
    var memory: String = "memory of ${connectionId.id}"

    // When set, reading the memory waits for it (a slow read).
    var memoryGate: CompletableDeferred<Unit>? = null

    private val events = Channel<IrohEvent>(Channel.UNLIMITED)

    var currentStatus = IrohConnectionStatus.CONNECTING
    var isShutDown = false
    var networkChangedCount = 0
    // What the connector was last told of the app (`setInBackground`).
    var toldInBackground = false
    var sendError: Exception? = null
    val sentMessages = mutableListOf<String>()

    // How many nextEvent() calls are waiting right now, and how many events were taken.
    var pendingReads = 0
    var takenEvents = 0

    // When set, shutting down waits for it (a slow close).
    var shutdownGate: CompletableDeferred<Unit>? = null

    // When set, sending waits for it (the peer confirming slowly).
    var sendGate: CompletableDeferred<Unit>? = null

    val isConnected: Boolean
        get() = currentStatus == IrohConnectionStatus.CONNECTED

    /**
     * Queues [event], as the library does. A state change is visible through [status] at once, like the real
     * session's state.
     */
    fun emit(event: IrohEvent) {
        if (event is IrohEvent.StateChanged) {
            currentStatus = event.status
        }
        events.trySend(event)
    }

    override fun setInBackground(inBackground: Boolean) {
        toldInBackground = inBackground
    }

    override suspend fun nextEvent(): IrohEvent? {
        pendingReads++
        try {
            return events.receiveCatching().getOrNull()?.also { takenEvents++ }
        } finally {
            pendingReads--
        }
    }

    override fun status(): IrohConnectionStatus = currentStatus

    var lastError: String? = null

    override fun diagnostics(): IrohSessionDiagnostics = IrohSessionDiagnostics(
        relayUrl = relayUrl ?: FakeIrohSessionFactory.PUBLIC_RELAY,
        usesPublicRelays = relayUrl == null,
        isRelayConnected = true,
        relayError = null,
        path = if (isConnected) IrohPath.DIRECT else null,
        rttMs = if (isConnected) 42 else null,
        failedAttempts = if (isConnected) 0 else 1,
        lastError = lastError,
        connectedForMs = if (isConnected) 1_000 else null,
        summary = "wallet 1111111111 → 2222222222 state=${currentStatus.name.lowercase()}"
    )

    override suspend fun send(message: String) {
        sendGate?.await()
        sendError?.let { throw it }
        if (isShutDown || !isConnected) throw MessageNotSentException("not connected")
        sentMessages += message
    }

    override suspend fun networkChanged() {
        networkChangedCount++
    }

    override suspend fun shutdownAndClose() {
        isShutDown = true
        events.close()
        shutdownGate?.await()
    }

    override suspend fun linkMemory(): String {
        memoryGate?.await()
        return memory
    }
}

internal class FakeIrohSessionFactory : IrohSessionFactory {

    val sessions = mutableListOf<FakeIrohSession>()
    var startError: Exception? = null

    // The link memories the library would read as not connected lately.
    val staleMemories = mutableSetOf<String>()
    var validationError: Exception? = null

    fun sessionsOf(password: RadixConnectPassword) = sessions.filter { it.connectionId == ConnectionIdHolder(password) }

    fun activeSessions() = sessions.filterNot { it.isShutDown }

    override fun publicRelayUrls(): List<String> = listOf(PUBLIC_RELAY)

    override fun validateRelayUrl(relayUrl: String): String? {
        validationError?.let { throw it }
        return if (relayUrl.startsWith("https://") || relayUrl.startsWith("http://")) null else "the scheme must be https or http"
    }

    override suspend fun startWalletSession(
        password: RadixConnectPassword,
        relayUrl: String?,
        linkMemory: String?
    ): IrohSessionHandle {
        startError?.let { throw it }
        return FakeIrohSession(connectionId = ConnectionIdHolder(password), relayUrl = relayUrl, startedWithMemory = linkMemory)
            .also { sessions += it }
    }

    override fun connectedLately(linkMemory: String): Boolean = linkMemory !in staleMemories

    companion object {
        const val PUBLIC_RELAY = "https://public.relay.example.com"
    }
}

internal class FakeNetworkChangeMonitor : NetworkChangeMonitor {

    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override fun changes(): Flow<Unit> = changes
}

/**
 * An [IrohConnector] that only records the pauses.
 */
internal class FakeIrohConnector(
    private val delegate: PeerdroidConnector = FakePeerdroidConnector()
) : IrohConnector, PeerdroidConnector by delegate {

    val pausedIds = mutableListOf<String>()
    val resumedIds = mutableListOf<String>()
    val deliveredMessages = mutableListOf<Pair<String, String>>()

    override fun deliverReceivedMessage(connectionId: ConnectionIdHolder, message: String) {
        deliveredMessages += connectionId.id to message
    }

    override suspend fun <T> withSessionPaused(connectionId: ConnectionIdHolder, block: suspend () -> T): T {
        pausedIds += connectionId.id
        try {
            return block()
        } finally {
            resumedIds += connectionId.id
        }
    }
}

/**
 * A linking transport whose behaviour is scripted by the test.
 */
class FakePeerdroidLink(
    private val behaviour: suspend (connectionListener: PeerdroidLink.ConnectionListener) -> Result<Unit>
) : PeerdroidLink {

    var addConnectionCount = 0
    var wasCancelled = false

    // A losing attempt is cancelled, possibly before it even started.
    val wasCancelledOrNeverStarted: Boolean
        get() = wasCancelled || addConnectionCount == 0
    val sentMessages = mutableListOf<String>()

    override suspend fun addConnection(
        p2pLink: com.radixdlt.sargon.P2pLink,
        p2pTransportProfile: P2pTransportProfile,
        connectionListener: PeerdroidLink.ConnectionListener
    ): Result<Unit> {
        addConnectionCount++
        try {
            return behaviour(connectionListener)
        } catch (exception: kotlinx.coroutines.CancellationException) {
            wasCancelled = true
            throw exception
        }
    }

    override suspend fun sendMessage(connectionId: String, message: PeerdroidLink.LinkClientExchangeInteraction): Result<Unit> {
        sentMessages += connectionId
        return Result.success(Unit)
    }
}
