package rdx.works.peerdroid.data

import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.LinkDiagnostics
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.RadixConnectDiagnostics
import java.util.concurrent.ConcurrentHashMap

/**
 * The [PeerdroidConnector] the app uses: connects every link over the transports its [TransportPolicy] gives it
 * (from [RadixConnectTransportSettings.transport] and the links known to support iroh), and follows the policy when
 * it changes at runtime.
 *
 * - Incoming messages of all transports are merged. On a link that uses more than one transport, a message that
 *   arrives again (the connector extension sends it again when its confirmation got lost, possibly over the other
 *   transport) is dropped: see [IncomingMessageDeduplicator]. A link over one transport gets every message, as before.
 * - The status of a connector merges the statuses of its transports: [PeerConnectionStatus.OPEN] if any is open,
 *   else [PeerConnectionStatus.CONNECTING] if any is connecting, else [PeerConnectionStatus.CLOSED].
 * - A message is sent once per connector: over its connected transports first, in [PeerdroidTransport] order (iroh
 *   first). The next transport is tried only when the previous one did not send it at all
 *   ([MessageNotSentException]), never when it may have been delivered.
 * - A link becomes known to support iroh as soon as its iroh session connects, whatever the mode.
 * - Its lock is never held across network I/O.
 * - Everything is logged following [RadixConnectLog], and kept per link for [linkDiagnostics].
 *
 * @param monotonicMillis a monotonic clock in milliseconds, for durations and for spotting repeated messages.
 */
@Suppress("TooManyFunctions")
internal class CompositePeerdroidConnector(
    private val connectors: Map<PeerdroidTransport, PeerdroidConnector>,
    private val settings: RadixConnectTransportSettings,
    private val applicationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    private val monotonicMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }
) : PeerdroidConnector, RadixConnectDiagnostics {

    // Not a data class: it holds the password, which must never end up in a log through toString().
    private class LinkRequest(val p2pTransportProfile: P2pTransportProfile, val password: RadixConnectPassword)

    // Keeps the requested links and the active policy consistent with each other. Never held across network I/O.
    private val mutex = Mutex()

    // Every link requested through connectToConnectorExtension, so that a transport enabled later can connect it.
    private val requestedLinks = ConcurrentHashMap<ConnectionIdHolder, LinkRequest>()

    // The policy the connections follow; null until known. Written while holding the mutex.
    private val activePolicy = MutableStateFlow<TransportPolicy?>(null)

    // The links being connected right now, to spot the same link connected twice at once.
    private val connectingLinks = ConcurrentHashMap<ConnectionIdHolder, Int>()

    private val events = LinkEventLog()
    private val logThrottle = LogThrottle()
    private val deduplicator = IncomingMessageDeduplicator(monotonicMillis = monotonicMillis)

    override val anyChannelConnected: Flow<Boolean> = combine(
        activePolicy,
        combine(connectors.map { (transport, connector) -> connector.anyChannelConnected.map { transport to it } }) { it.toMap() }
    ) { policy, isConnectedByTransport ->
        isConnectedByTransport.any { (transport, isConnected) -> isConnected && policy?.mayUse(transport) != false }
    }.distinctUntilChanged()

    override val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>> = combine(
        activePolicy,
        combine(connectors.map { (transport, connector) -> connector.peerConnectionStatus.map { transport to it } }) { it.toMap() }
    ) { policy, statusesByTransport ->
        // Only the transports the policy gives each link: a transport being switched off is not reported anymore.
        mergeStatuses(
            statusesByTransport.map { (transport, statuses) ->
                if (policy == null) {
                    statuses
                } else {
                    statuses.filterKeys { connectorId -> transport in policy.transportsFor(ConnectionIdHolder(connectorId)) }
                }
            }
        )
    }.distinctUntilChanged()

    override val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent> = connectors
        .map { (transport, connector) -> connector.dataChannelMessagesFromRemoteClients.map { event -> transport to event } }
        .merge()
        .filter { (transport, event) -> accept(transport, event) }
        .map { (_, event) -> event }
        .shareIn(scope = applicationScope, started = SharingStarted.WhileSubscribed())

    init {
        applicationScope.launch(ioDispatcher) {
            combine(settings.transport, settings.irohCapableConnectionIds) { mode, irohCapableConnectionIds ->
                TransportPolicy(mode = mode, irohCapableConnectionIds = irohCapableConnectionIds)
            }
                .distinctUntilChanged()
                .collect { policy -> applyPolicy(policy) }
        }
        connectors[PeerdroidTransport.IROH]?.let { irohConnector ->
            applicationScope.launch(ioDispatcher) { learnIrohSupport(irohConnector) }
        }
        applicationScope.launch(ioDispatcher) { logStatusChanges() }
    }

    override suspend fun connectToConnectorExtension(
        p2pTransportProfile: P2pTransportProfile,
        encryptionKey: RadixConnectPassword
    ): Result<Unit> {
        val connectionId = ConnectionIdHolder(encryptionKey)
        val request = LinkRequest(p2pTransportProfile = p2pTransportProfile, password = encryptionKey)
        val policy = mutex.withLock {
            requestedLinks[connectionId] = request
            resolvePolicy()
        }
        val transports = policy.transportsFor(connectionId)
        RadixConnectLog.i("🔀 ${connectionId.logId} connecting: transports=${transports.logNames()} (${policy.describeFor(connectionId)})")
        events.record(connectionId, SOURCE_LINK, "connecting over ${transports.logNames()} (${policy.describeFor(connectionId)})")
        return connectStillWanted(transports = transports, connectionId = connectionId, request = request)
    }

    override suspend fun deleteConnector(connectionId: ConnectionIdHolder) {
        RadixConnectLog.i("🔀 ${connectionId.logId} deleting: stopping every transport")
        mutex.withLock { requestedLinks.remove(connectionId) }
        // All of them, whatever the policy: removes anything left over from a previous policy too.
        connectors.values.forEach { connector -> connector.deleteConnector(connectionId) }
        runCatchingCancellable { settings.forgetLink(connectionId) }
            .onSuccess { RadixConnectLog.d("🔀 ${connectionId.logId} forgot what was learned about the link (iroh support)") }
            .onFailure { error ->
                RadixConnectLog.w("🔀 ${connectionId.logId} failed to forget the link: ${error.javaClass.simpleName}")
            }
        events.forget(connectionId)
        deduplicator.forget(connectionId)
        logThrottle.forget(connectionId.id)
    }

    override fun terminateConnectionToConnectorExtension() {
        RadixConnectLog.i("🔀 terminating every connection (${requestedLinks.size} links)")
        requestedLinks.keys.forEach { connectionId -> events.record(connectionId, SOURCE_LINK, "every connection terminated") }
        // Synchronously, like the transports do, so that links connected right after this call are kept.
        requestedLinks.clear()
        connectors.values.forEach { connector -> connector.terminateConnectionToConnectorExtension() }
    }

    override suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit> {
        val transports = currentPolicy().transportsFor(ConnectionIdHolder(remoteConnectorId))
        return sendOnce(connectorId = remoteConnectorId, transports = transports, message = message)
    }

    override suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit> {
        val policy = currentPolicy()
        val connectedConnectorIds = connectors.flatMap { (transport, connector) ->
            connector.peerConnectionStatus.first()
                .filter { (connectorId, status) ->
                    status == PeerConnectionStatus.OPEN && transport in policy.transportsFor(ConnectionIdHolder(connectorId))
                }
                .keys
        }.toSet()
        if (connectedConnectorIds.isEmpty()) {
            RadixConnectLog.e("🔀 could not send ${describeMessage(message)} to every link: none is connected")
            return Result.failure(MessageNotSentException("no connector is connected"))
        }

        RadixConnectLog.d("🔀 sending ${describeMessage(message)} to the ${connectedConnectorIds.size} connected links")
        val results = coroutineScope {
            connectedConnectorIds.map { connectorId ->
                async { sendOnce(connectorId, policy.transportsFor(ConnectionIdHolder(connectorId)), message) }
            }.awaitAll()
        }
        return results.firstOrNull { it.isSuccess } ?: run {
            RadixConnectLog.e("🔀 could not send ${describeMessage(message)} to any of the ${connectedConnectorIds.size} connected links")
            // A failure that may have delivered the message wins over "not sent", so no transport sends it again.
            results.firstOrNull { it.exceptionOrNull() !is MessageNotSentException } ?: results.first()
        }
    }

    override fun onAppForeground() {
        RadixConnectLog.i("🔀 app foreground: asking the transports to check their connections (${requestedLinks.size} links)")
        connectors.values.forEach { connector -> connector.onAppForeground() }
    }

    // iroh stops what costs battery and tells its connectors; WebRTC keeps its connections and tells its connectors.
    override fun onAppBackground() {
        RadixConnectLog.i("🔀 app background: telling the transports (${requestedLinks.size} links)")
        connectors.values.forEach { connector -> connector.onAppBackground() }
    }

    override fun onAppInFront(inFront: Boolean) {
        RadixConnectLog.i("🔀 app ${if (inFront) "in front" else "no longer in front"}: telling the transports")
        connectors.values.forEach { connector -> connector.onAppInFront(inFront) }
    }

    override suspend fun linkDiagnostics(): List<LinkDiagnostics> {
        val policy = currentPolicy()
        val statuses = peerConnectionStatus.first()
        return requestedLinks.keys
            .sortedBy { it.id }
            .map { connectionId ->
                LinkDiagnostics(
                    connectionId = connectionId.id,
                    status = statuses[connectionId.id],
                    transports = policy.transportsFor(connectionId)
                        .sortedBy { it.ordinal }
                        .mapNotNull { transport -> connectors[transport]?.diagnostics(connectionId) },
                    // Every transport's, even one the link no longer uses: what happened to it still explains a lot.
                    recentEvents = (events.recent(connectionId) + connectors.values.flatMap { it.recentEvents(connectionId) })
                        .sortedBy { it.atMillis }
                        .takeLast(MAX_REPORTED_EVENTS)
                )
            }
    }

    // Sends [message] once to [connectorId]: over its connected transports first, in preference order, moving on to
    // the next transport only while nothing was sent.
    private suspend fun sendOnce(connectorId: String, transports: Set<PeerdroidTransport>, message: String): Result<Unit> {
        val connectionId = ConnectionIdHolder(connectorId)
        val connectedTransports = transports.filter { transport ->
            connectors[transport]?.peerConnectionStatus?.first()?.get(connectorId) == PeerConnectionStatus.OPEN
        }
        val orderedTransports = transports.sortedWith(compareBy<PeerdroidTransport> { it !in connectedTransports }.thenBy { it.ordinal })
        val messageDescription = describeMessage(message)
        val startedAt = monotonicMillis()

        var result: Result<Unit> = Result.failure(MessageNotSentException("no transport for ${connectionId.logId}"))
        var lastTriedTransport: PeerdroidTransport? = null
        val availableTransports = orderedTransports.mapNotNull { transport -> connectors[transport]?.let { transport to it } }
        for ((transport, connector) in availableTransports) {
            lastTriedTransport?.let { previous ->
                val fallback = "${previous.logName} did not send it (${result.exceptionOrNull()?.sendFailureCategory()}), " +
                    "falling back to ${transport.logName}"
                RadixConnectLog.w("🔀 ${connectionId.logId} $messageDescription: $fallback")
                events.record(connectionId, SOURCE_LINK, "message fell back from ${previous.logName} to ${transport.logName}")
            }
            lastTriedTransport = transport
            result = try {
                connector.sendDataChannelMessageToRemoteClient(connectorId, message)
            } catch (exception: CancellationException) {
                RadixConnectLog.w(
                    "🔀 ${connectionId.logId} sending $messageDescription over ${transport.logName} cancelled " +
                        "after ${monotonicMillis() - startedAt} ms"
                )
                throw exception
            }
            if (result.exceptionOrNull() !is MessageNotSentException) break
        }

        val elapsedMs = monotonicMillis() - startedAt
        val route = "connected: ${connectedTransports.logNames()}, order: ${orderedTransports.logNames()}"
        result.onSuccess {
            RadixConnectLog.i(
                "🔀 ${connectionId.logId} sent $messageDescription over ${lastTriedTransport?.logName} in $elapsedMs ms ($route)"
            )
        }.onFailure { error ->
            RadixConnectLog.e(
                "🔀 ${connectionId.logId} could not send $messageDescription after $elapsedMs ms: " +
                    "${lastTriedTransport?.logName ?: "no transport"} failed (${error.sendFailureCategory()}) ($route)"
            )
            events.record(connectionId, SOURCE_LINK, "a message could not be sent (${error.sendFailureCategory()})")
        }
        return result
    }

    // Whether an incoming event goes to the app. Drops a message that arrives again.
    private fun accept(transport: PeerdroidTransport, event: DataChannelWrapperEvent): Boolean = when (event) {
        is DataChannelWrapperEvent.MessageFromRemoteConnectionId -> acceptMessage(transport, event)
        is DataChannelWrapperEvent.Error -> {
            val connectionId = event.connectionIdHolder
            val line = "🔀 ${connectionId.logId} rejected an invalid incoming message over ${transport.logName}"
            logThrottle.log(connectionId.id, line) { RadixConnectLog.w(it) }
            true
        }

        is DataChannelWrapperEvent.StateChangedForRemoteConnector -> {
            RadixConnectLog.d("🔀 ${event.connectionIdHolder.logId} ${transport.logName} data channel ${event.state.name.lowercase()}")
            true
        }
    }

    private fun acceptMessage(transport: PeerdroidTransport, event: DataChannelWrapperEvent.MessageFromRemoteConnectionId): Boolean {
        val connectionId = event.connectionIdHolder
        val message = event.messageInJsonString
        val messageDescription = describeMessage(message)
        // Only an interaction can be told apart from another one that happens to have the same contents, and only a link
        // over several transports drops repeats.
        val usesSeveralTransports = (activePolicy.value?.transportsFor(connectionId)?.size ?: 1) > 1
        val previous = interactionIdOf(message)
            ?.takeIf { usesSeveralTransports }
            ?.let { deduplicator.previousSighting(connectionId, message, transport) }
        if (previous == null) {
            RadixConnectLog.i("🔀 ${connectionId.logId} received $messageDescription over ${transport.logName}")
            return true
        }

        val agoMs = monotonicMillis() - previous.atMillis
        logThrottle.log(
            connectionId.id,
            "🔀 ${connectionId.logId} dropped a duplicate of $messageDescription over ${transport.logName}: " +
                "first received over ${previous.transport.logName} $agoMs ms earlier"
        ) { line -> RadixConnectLog.w(line) }
        events.record(
            connectionId,
            SOURCE_LINK,
            "duplicate message over ${transport.logName} dropped (first received over ${previous.transport.logName})"
        )
        return false
    }

    // Called for one policy change at a time (the settings are collected sequentially).
    private suspend fun applyPolicy(policy: TransportPolicy) {
        val (previous, links) = mutex.withLock {
            val previous = activePolicy.value
            activePolicy.value = policy
            previous to requestedLinks.toMap()
        }
        // Nothing was connected before the policy was known: connectToConnectorExtension resolves it first.
        if (previous == null || previous == policy) return

        RadixConnectLog.i(
            "🔀 transport policy changed: mode=${policy.mode.name.lowercase()}, " +
                "links supporting iroh: ${policy.irohCapableConnectionIds.size}, links to update: ${links.size}"
        )
        links.forEach { (connectionId, request) ->
            val previousTransports = previous.transportsFor(connectionId)
            val transports = policy.transportsFor(connectionId)
            val removedTransports = previousTransports - transports
            val addedTransports = transports - previousTransports
            if (removedTransports.isNotEmpty() || addedTransports.isNotEmpty()) {
                val change = "transports ${previousTransports.logNames()} → ${transports.logNames()} (${policy.describeFor(connectionId)})"
                RadixConnectLog.i("🔀 ${connectionId.logId} $change")
                events.record(connectionId, SOURCE_LINK, change)
            }
            // Awaited, link by link (a terminate would finish asynchronously), so that a transport enabled again
            // right after starts from a clean state.
            removedTransports.forEach { transport -> connectors[transport]?.deleteConnector(connectionId) }
            if (addedTransports.isNotEmpty()) {
                connectStillWanted(addedTransports, connectionId, request)
            }
        }
    }

    // Connects without holding the mutex, then undoes it on the transports that are no longer wanted because the
    // link was deleted (or terminated), or the policy changed, in the meantime.
    private suspend fun connectStillWanted(
        transports: Set<PeerdroidTransport>,
        connectionId: ConnectionIdHolder,
        request: LinkRequest
    ): Result<Unit> {
        val concurrentConnects = connectingLinks.merge(connectionId, 1, Int::plus) ?: 1
        if (concurrentConnects > 1) {
            RadixConnectLog.w(
                "🔀 ${connectionId.logId} is being connected $concurrentConnects times at once: each transport keeps one connection"
            )
        }
        val result = try {
            connect(transports, connectionId, request)
        } finally {
            connectingLinks.computeIfPresent(connectionId) { _, count -> (count - 1).takeIf { it > 0 } }
        }
        val isRequested = requestedLinks.containsKey(connectionId)
        val wantedTransports = activePolicy.value?.transportsFor(connectionId)
        val unwantedTransports = transports.filter { transport ->
            !isRequested || (wantedTransports != null && transport !in wantedTransports)
        }
        if (unwantedTransports.isNotEmpty()) {
            val reason = if (isRequested) "the transport policy changed" else "the link was deleted or terminated"
            RadixConnectLog.i(
                "🔀 ${connectionId.logId} stopping ${unwantedTransports.logNames()} right after connecting: $reason meanwhile"
            )
        }
        unwantedTransports.forEach { transport -> connectors[transport]?.deleteConnector(connectionId) }
        return result
    }

    // Succeeds if any of the transports connected.
    private suspend fun connect(
        transports: Set<PeerdroidTransport>,
        connectionId: ConnectionIdHolder,
        request: LinkRequest
    ): Result<Unit> {
        val targets = transports.sortedBy { it.ordinal }.mapNotNull { transport -> connectors[transport]?.let { transport to it } }
        if (targets.isEmpty()) {
            RadixConnectLog.e("🔀 ${connectionId.logId} could not connect: no transport is available")
            return Result.failure(IllegalStateException("no transport is available for this link"))
        }

        val startedAt = monotonicMillis()
        suspend fun connectOver(target: Pair<PeerdroidTransport, PeerdroidConnector>): Triple<PeerdroidTransport, Result<Unit>, Long> {
            val result = target.second.connectToConnectorExtension(request.p2pTransportProfile, request.password)
            return Triple(target.first, result, monotonicMillis() - startedAt)
        }

        val results = if (targets.size == 1) {
            listOf(connectOver(targets.single()))
        } else {
            coroutineScope { targets.map { target -> async { connectOver(target) } }.awaitAll() }
        }
        val summary = results.joinToString { (transport, result, elapsedMs) ->
            val outcome = result.exceptionOrNull()?.let { error -> "failed (${error.describe()})" } ?: "started"
            "${transport.logName} $outcome in $elapsedMs ms"
        }
        if (results.any { (_, result, _) -> result.isSuccess }) {
            RadixConnectLog.i("🔀 ${connectionId.logId} connect: $summary")
        } else {
            RadixConnectLog.w("🔀 ${connectionId.logId} connect failed on every transport: $summary")
            events.record(connectionId, SOURCE_LINK, "could not start any transport")
        }
        return results.firstOrNull { (_, result, _) -> result.isSuccess }?.second ?: results.first().second
    }

    // A link whose iroh session connects supports iroh, whatever the mode.
    private suspend fun learnIrohSupport(irohConnector: PeerdroidConnector) {
        irohConnector.peerConnectionStatus.collect { statuses ->
            statuses
                .filter { (connectorId, status) ->
                    status == PeerConnectionStatus.OPEN &&
                        activePolicy.value?.irohCapableConnectionIds?.contains(connectorId) != true
                }
                .keys
                .forEach { connectorId ->
                    val connectionId = ConnectionIdHolder(connectorId)
                    runCatchingCancellable { settings.rememberIrohSupport(connectionId) }
                        .onSuccess {
                            val line = "🔀 ${connectionId.logId} iroh support learned: its iroh session connected"
                            logThrottle.log(connectorId, line) { text -> RadixConnectLog.i(text) }
                        }
                        .onFailure { error ->
                            RadixConnectLog.w("🔀 ${connectionId.logId} failed to remember iroh support: ${error.javaClass.simpleName}")
                        }
                }
        }
    }

    // Logs every change of the merged status of a link, with the status of each transport.
    private suspend fun logStatusChanges() {
        var previousStatuses = emptyMap<String, PeerConnectionStatus>()
        val lostAt = mutableMapOf<String, Long>()
        peerConnectionStatus.collect { statuses ->
            (previousStatuses.keys + statuses.keys).forEach { connectorId ->
                val previous = previousStatuses[connectorId]
                val status = statuses[connectorId]
                if (previous != status) {
                    val connectionId = ConnectionIdHolder(connectorId)
                    val change = "${previous.describe()} → ${status.describe()}"
                    val downtime = when {
                        previous == PeerConnectionStatus.OPEN -> {
                            lostAt[connectorId] = monotonicMillis()
                            ""
                        }

                        status == PeerConnectionStatus.OPEN -> lostAt.remove(connectorId)
                            ?.let { since -> ", connected again after ${monotonicMillis() - since} ms" }
                            .orEmpty()

                        else -> ""
                    }
                    RadixConnectLog.i("🔀 ${connectionId.logId} status: $change (${transportStatuses(connectorId)}$downtime)")
                    events.record(connectionId, SOURCE_LINK, "status $change")
                }
            }
            previousStatuses = statuses
        }
    }

    private suspend fun transportStatuses(connectorId: String): String = connectors.entries
        .sortedBy { (transport, _) -> transport.ordinal }
        .map { (transport, connector) -> "${transport.logName}=${connector.peerConnectionStatus.first()[connectorId].describe()}" }
        .joinToString()

    // Requires the mutex.
    private suspend fun resolvePolicy(): TransportPolicy = activePolicy.value
        ?: readPolicy().also { policy -> activePolicy.value = policy }

    private suspend fun currentPolicy(): TransportPolicy = activePolicy.value ?: readPolicy()

    private suspend fun readPolicy() = TransportPolicy(
        mode = settings.transport.first(),
        irohCapableConnectionIds = settings.irohCapableConnectionIds.first()
    )

    private fun TransportPolicy.describeFor(connectionId: ConnectionIdHolder): String =
        "mode=${mode.name.lowercase()}, irohCapable=${connectionId.id in irohCapableConnectionIds}"

    private fun Collection<PeerdroidTransport>.logNames(): List<String> = sortedBy { it.ordinal }.map { it.logName }

    private fun PeerConnectionStatus?.describe(): String = this?.name?.lowercase() ?: "not running"

    companion object {

        private const val SOURCE_LINK = "link"
        private const val MAX_REPORTED_EVENTS = 10
        private const val NANOS_PER_MILLI = 1_000_000L

        internal fun mergeStatuses(statuses: List<Map<String, PeerConnectionStatus>>): Map<String, PeerConnectionStatus> =
            statuses.flatMap { it.keys }.toSet().associateWith { connectorId ->
                val connectorStatuses = statuses.mapNotNull { it[connectorId] }
                when {
                    PeerConnectionStatus.OPEN in connectorStatuses -> PeerConnectionStatus.OPEN
                    PeerConnectionStatus.CONNECTING in connectorStatuses -> PeerConnectionStatus.CONNECTING
                    else -> PeerConnectionStatus.CLOSED
                }
            }
    }
}
