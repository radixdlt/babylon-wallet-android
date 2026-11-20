@file:Suppress("TooManyFunctions")

package rdx.works.peerdroid.data.iroh

import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.RadixConnectPassword
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rdx.works.peerdroid.data.LinkEventLog
import rdx.works.peerdroid.data.LogThrottle
import rdx.works.peerdroid.data.PeerdroidConnector
import rdx.works.peerdroid.data.RadixConnectLog
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.data.describe
import rdx.works.peerdroid.data.logId
import rdx.works.peerdroid.data.runCatchingCancellable
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.LinkEvent
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.PeerConnectionStatus
import rdx.works.peerdroid.domain.TransportDiagnostics
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The iroh transport of Radix Connect.
 */
internal interface IrohConnector : PeerdroidConnector {

    /**
     * Runs [block] while the session of [connectionId] is stopped, so that another session using the same password,
     * hence the same iroh endpoint key (the linking session), does not compete with it at the relay.
     *
     * Afterwards the session is started again if it was running or was requested in the meantime, unless the link was
     * deleted or the connector terminated in the meantime.
     */
    suspend fun <T> withSessionPaused(connectionId: ConnectionIdHolder, block: suspend () -> T): T

    /**
     * Hands over a [message] another session of the link [connectionId] (the linking session) took, hence confirmed
     * to the connector extension, exactly like a message of the regular session: the extension will not send it
     * again, so it must not be dropped.
     */
    fun deliverReceivedMessage(connectionId: ConnectionIdHolder, message: String)
}

/**
 * [PeerdroidConnector] over iroh: one wallet session per linked connector extension, keyed like the WebRTC
 * connections by [ConnectionIdHolder] (the hash of the link's password).
 *
 * - The sessions stop while the app is in the background (see [IrohBackgroundStop]) and start again when it comes
 *   back, when the time it took each link to connect is logged.
 *
 * - Connection states are reported as [PeerConnectionStatus.OPEN] when connected and [PeerConnectionStatus.CLOSED]
 *   otherwise. A wallet session stays "connecting" for as long as it keeps dialing a connector extension that is not
 *   running (WebRTC reports that case as closed or not at all), and consumers such as
 *   `PeerdroidClient.openConnectionIds` wait until no connection is [PeerConnectionStatus.CONNECTING].
 * - Each link has its own lock: a new session never starts before the previous one (which has the same endpoint key)
 *   finished closing, and the slow close of one link never holds up the others.
 * - The library confirms a message to the connector extension when it is taken from the session. Events are only
 *   taken while somebody collects [dataChannelMessagesFromRemoteClients]; meanwhile messages stay unconfirmed (the
 *   extension retries them) and the connection status is polled instead.
 */
@Suppress("LargeClass") // One class per transport, like PeerdroidConnectorImpl: the rules above share its state.
internal class IrohPeerdroidConnector(
    private val sessionFactory: IrohSessionFactory,
    private val settings: RadixConnectTransportSettings,
    private val networkChangeMonitor: NetworkChangeMonitor,
    private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val monotonicMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }
) : IrohConnector {

    // Not a data class: it holds the password, which must never end up in a log through toString().
    private class Session(
        val connectionId: ConnectionIdHolder,
        val password: RadixConnectPassword,
        val relayUrl: String?,
        val generation: Long,
        val handle: IrohSessionHandle
    ) {
        var eventsJob: Job? = null

        // The last connection state logged for this session.
        @Volatile
        var loggedConnectionStatus: IrohConnectionStatus? = null
    }

    // A link to start once its pause ends.
    private class ResumeRequest(val password: RadixConnectPassword, val generation: Long)

    // The lifecycle of one link. Its mutex serialises the link's start, stop, pause and resume.
    private class Link {
        val mutex = Mutex()

        // The fields are written while holding the mutex.
        @Volatile
        var session: Session? = null
        var pauseCount = 0
        var resumeRequest: ResumeRequest? = null

        // Stopped (or kept from starting) while the app is in the background: started again when it comes back.
        @Volatile
        var isStoppedForBackground = false

        // Whether a session of this link has been connected since the app started: such links go first when the
        // sessions start again (see restartAfterBackground).
        @Volatile
        var hasConnected = false

        // Not connected lately, at a cold start: started once the live links are connected (see startsAfterLiveLinks).
        @Volatile
        var waitsForLiveLinks = false
    }

    // A message confirmed to the connector extension already, to hand over to the app.
    private class Delivery(val event: DataChannelWrapperEvent, val delivered: CompletableDeferred<Unit>)

    private sealed interface NextEvent {
        class Taken(val event: IrohEvent) : NextEvent
        data object CollectorsGone : NextEvent
        data object Ended : NextEvent
    }

    private val links = ConcurrentHashMap<ConnectionIdHolder, Link>()

    // The running sessions, for reads without locking. Written while holding the link's mutex.
    private val sessions = ConcurrentHashMap<ConnectionIdHolder, Session>()

    // Incremented by every terminate: sessions and resume requests of older generations are dropped.
    private val generation = AtomicLong(0)

    private val watchLock = Any()

    // Guarded by watchLock: observes network and relay changes while any session runs.
    private var watchJob: Job? = null

    private val status = MutableStateFlow(emptyMap<String, PeerConnectionStatus>())

    // Why a link could not be started or a message not sent, for the diagnostics (the library keeps the rest).
    private val lastErrors = ConcurrentHashMap<ConnectionIdHolder, String>()

    private val logThrottle = LogThrottle()

    private val events = LinkEventLog()

    // No buffer: emitting returns once a collector has the message.
    private val messages = MutableSharedFlow<DataChannelWrapperEvent>()

    override val anyChannelConnected: Flow<Boolean> = status
        .map { statuses -> statuses.containsValue(PeerConnectionStatus.OPEN) }
        .distinctUntilChanged()

    override val peerConnectionStatus: Flow<Map<String, PeerConnectionStatus>> = status.asStateFlow()

    override val dataChannelMessagesFromRemoteClients: SharedFlow<DataChannelWrapperEvent> = messages.asSharedFlow()

    // Why the running sessions should check their connection, besides network changes (e.g. the app foreground).
    private val connectionCheckRequests = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Every message confirmed to a connector extension (by the regular sessions or the linking ones), handed over in
    // order by a single coroutine. A message stays here when its session stops: it is never dropped.
    private val deliveries = Channel<Delivery>(Channel.UNLIMITED)

    private val inFlight = InFlightExchanges()

    private val backgroundStop = IrohBackgroundStop(
        applicationScope = applicationScope,
        ioDispatcher = ioDispatcher,
        inFlight = inFlight,
        stopSessions = { stopForBackground() },
        restartSessions = { restartAfterBackground() }
    )

    // A cold start (the app's process just started): the links connected in the last day start first, the others once
    // those are connected, or after FIRST_LINKS_HEAD_START at most, as when the app comes back to the foreground.
    @Volatile
    private var coldStartOver = false
    private val liveAtColdStart: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val dormantLinksScheduled = AtomicBoolean(false)

    // When the app last came to the foreground, per link not connected over iroh since.
    private val foregroundSince = ConcurrentHashMap<ConnectionIdHolder, Long>()

    init {
        applicationScope.launch(ioDispatcher) {
            for (delivery in deliveries) {
                // Confirmed already: wait for a collector rather than drop it.
                messages.subscriptionCount.first { count -> count > 0 }
                messages.emit(delivery.event)
                delivery.delivered.complete(Unit)
                inFlight.end()
            }
        }
    }

    override suspend fun connectToConnectorExtension(
        p2pTransportProfile: P2pTransportProfile,
        encryptionKey: RadixConnectPassword
    ): Result<Unit> = withContext(ioDispatcher) {
        val connectionId = ConnectionIdHolder(encryptionKey)
        val link = link(connectionId)
        val requestGeneration = generation.get()
        val result = link.mutex.withLock {
            val session = link.session
            when {
                session != null && session.generation >= requestGeneration -> {
                    RadixConnectLog.d("🧊 ${connectionId.logId} iroh session already running: kept")
                    Result.success(Unit)
                }

                link.pauseCount > 0 || backgroundStop.isStopped -> {
                    val reason = if (link.pauseCount > 0) "the link is being linked" else "the app is in the background"
                    RadixConnectLog.i("🧊 ${connectionId.logId} iroh session start deferred: $reason")
                    if (backgroundStop.isStopped) link.isStoppedForBackground = true
                    link.resumeRequest = ResumeRequest(password = encryptionKey, generation = requestGeneration)
                    Result.success(Unit)
                }

                else -> {
                    // A session from before the last terminate is being dropped by it: replace it now.
                    session?.let { stopSession(link, it, reason = "replaced by a new connection after a terminate") }
                    if (startsAfterLiveLinks(connectionId)) {
                        RadixConnectLog.i(
                            "🧊 ${connectionId.logId} iroh session start deferred: not connected lately, after the live links"
                        )
                        link.waitsForLiveLinks = true
                        link.resumeRequest = ResumeRequest(password = encryptionKey, generation = requestGeneration)
                        startDormantLinksLater()
                        Result.success(Unit)
                    } else {
                        startSession(link, connectionId, encryptionKey, requestGeneration, reason = "connect")
                    }
                }
            }
        }
        updateWatching()
        result
    }

    override suspend fun deleteConnector(connectionId: ConnectionIdHolder) {
        withContext(ioDispatcher) {
            val link = links[connectionId]
            lastErrors.remove(connectionId)
            foregroundSince.remove(connectionId)
            logThrottle.forget(connectionId.id)
            logThrottle.forget(connectionId.id + NETWORK_LOG_KEY_SUFFIX)
            if (link == null) {
                status.update { statuses -> statuses - connectionId.id }
            } else {
                link.mutex.withLock {
                    link.resumeRequest = null
                    link.isStoppedForBackground = false
                    link.session?.let { session ->
                        stopSession(link, session, reason = "the link is deleted or no longer uses iroh", keepMemory = false)
                    }
                    status.update { statuses -> statuses - connectionId.id }
                }
            }
            updateWatching()
        }
    }

    override fun terminateConnectionToConnectorExtension() {
        // Everything from before this call is dropped, link by link and concurrently; a link connected after it is
        // kept (it gets the new generation).
        val terminatedGeneration = generation.getAndIncrement()
        RadixConnectLog.i("🧊 terminating every iroh session (${sessions.size} running)")
        applicationScope.launch(ioDispatcher) {
            coroutineScope {
                links.forEach { (connectionId, link) ->
                    launch { terminateLink(connectionId, link, terminatedGeneration) }
                }
            }
            updateWatching()
        }
    }

    override suspend fun sendDataChannelMessageToRemoteClient(remoteConnectorId: String, message: String): Result<Unit> {
        val session = sessions[ConnectionIdHolder(remoteConnectorId)]
            ?: return Result.failure(MessageNotSentException("no iroh session for ${ConnectionIdHolder(remoteConnectorId).logId}"))
        return send(session, message)
    }

    override suspend fun sendDataChannelMessageToAllRemoteClients(message: String): Result<Unit> {
        val connectedSessions = sessions.values.filter { session ->
            status.value[session.connectionId.id] == PeerConnectionStatus.OPEN
        }
        if (connectedSessions.isEmpty()) {
            return Result.failure(MessageNotSentException("no connector is connected over iroh"))
        }

        val results = coroutineScope {
            connectedSessions.map { session -> async { send(session, message) } }.awaitAll()
        }
        return results.firstOrNull { it.isSuccess }
            // A failure that may have delivered the message wins over "not sent", so nobody sends it again.
            ?: results.firstOrNull { it.exceptionOrNull() !is MessageNotSentException }
            ?: results.first()
    }

    override suspend fun <T> withSessionPaused(connectionId: ConnectionIdHolder, block: suspend () -> T): T {
        // Not cancellable, so that a pause that took effect is always followed by its resume.
        withContext(NonCancellable) { pause(connectionId) }
        try {
            return block()
        } finally {
            withContext(NonCancellable) { resume(connectionId) }
        }
    }

    override fun diagnostics(connectionId: ConnectionIdHolder): TransportDiagnostics.Iroh {
        val sessionDiagnostics = sessions[connectionId]?.let { session ->
            runCatchingCancellable { session.handle.diagnostics() }.getOrNull()
        }
        return TransportDiagnostics.Iroh(
            status = status.value[connectionId.id],
            lastError = sessionDiagnostics?.lastError ?: lastErrors[connectionId],
            session = sessionDiagnostics
        )
    }

    override fun recentEvents(connectionId: ConnectionIdHolder): List<LinkEvent> = events.recent(connectionId)

    // Restarts the sessions stopped in the background. A session kept running may be dead without knowing it until
    // the QUIC idle timeout (the process was frozen): like a network change, it makes every session check its
    // connection at once and redial if it is dead.
    override fun onAppForeground() {
        val now = monotonicMillis()
        links.forEach { (connectionId, link) ->
            if (link.session != null || link.isStoppedForBackground) foregroundSince[connectionId] = now
        }
        backgroundStop.onAppForeground()
        if (sessions.isEmpty()) {
            RadixConnectLog.d("🧊 app foreground: no running iroh session to check")
            return
        }
        connectionCheckRequests.tryEmit(REASON_APP_FOREGROUND)
    }

    override fun onAppBackground() {
        tellPresence(inBackground = true)
        backgroundStop.onAppBackground()
    }

    override fun onAppInFront(inFront: Boolean) = tellPresence(inBackground = !inFront)

    // Whether the app is out of the person's sight (not in front), as the connectors are told: on every change, and by
    // every session started meanwhile. A connector asking several wallets then gives a request to the one the person
    // opens, and a wallet out of sight that stops answering has been closed.
    @Volatile
    private var appInBackground = false

    private fun tellPresence(inBackground: Boolean) {
        if (appInBackground == inBackground) return
        appInBackground = inBackground
        sessions.values.forEach { session -> runCatching { session.handle.setInBackground(inBackground) } }
    }

    override fun deliverReceivedMessage(connectionId: ConnectionIdHolder, message: String) {
        // The size only: never the contents.
        RadixConnectLog.i("🧊 ${connectionId.logId} handing over a message (${message.length} chars) received while linking")
        enqueue(DataChannelWrapperEvent.MessageFromRemoteConnectionId(connectionIdHolder = connectionId, messageInJsonString = message))
    }

    private fun enqueue(event: DataChannelWrapperEvent): Delivery {
        val delivery = Delivery(event = event, delivered = CompletableDeferred())
        inFlight.begin()
        deliveries.trySend(delivery)
        return delivery
    }

    // Requires no lock. Stops every session until the app comes back; they are started again as they were.
    private suspend fun stopForBackground() {
        foregroundSince.clear()
        coroutineScope {
            links.forEach { (connectionId, link) ->
                launch {
                    link.mutex.withLock {
                        link.isStoppedForBackground = true
                        link.session?.let { session ->
                            if (session.generation >= generation.get()) {
                                link.resumeRequest = ResumeRequest(password = session.password, generation = session.generation)
                            }
                            stopSession(link, session, reason = REASON_APP_BACKGROUND)
                            setStatus(connectionId, PeerConnectionStatus.CLOSED)
                        }
                    }
                }
            }
        }
        updateWatching()
    }

    // During a cold start, whether the link [connectionId] waits for the live ones: its connector was not connected in
    // the last day (or never). Starting every session at once makes each of them slower: a radio just woken up carries
    // all their relay connections, address lookups and handshakes together (live links took 5-8 s instead of under
    // one, measured with 7 such links out of 10). A link without a memory starts at once.
    private suspend fun startsAfterLiveLinks(connectionId: ConnectionIdHolder): Boolean {
        if (coldStartOver) return false
        val memory = runCatchingCancellable { settings.irohLinkMemory(connectionId) }.getOrNull()
        val waits = memory != null && !runCatching { sessionFactory.connectedLately(memory) }.getOrDefault(true)
        if (!waits) liveAtColdStart += connectionId.id
        return waits
    }

    // Starts the links a cold start held back once the live ones are connected, or after FIRST_LINKS_HEAD_START.
    private fun startDormantLinksLater() {
        if (!dormantLinksScheduled.compareAndSet(false, true)) return
        applicationScope.launch(ioDispatcher) {
            withTimeoutOrNull(FIRST_LINKS_HEAD_START) {
                status.first { statuses ->
                    liveAtColdStart.isNotEmpty() && liveAtColdStart.all { id -> statuses[id] == PeerConnectionStatus.OPEN }
                }
            }
            coldStartOver = true
            startWaitingDormantLinks()
            updateWatching()
        }
    }

    private suspend fun startWaitingDormantLinks() = coroutineScope {
        links.forEach { (connectionId, link) ->
            if (!link.waitsForLiveLinks) return@forEach
            launch {
                link.mutex.withLock {
                    if (!link.waitsForLiveLinks) return@withLock
                    link.waitsForLiveLinks = false
                    val request = link.resumeRequest
                    // Deleted, or being linked (it starts when the linking ends), meanwhile.
                    if (link.pauseCount > 0 || request == null) return@withLock
                    // In the background: it starts when the app comes back.
                    if (backgroundStop.isStopped) {
                        link.isStoppedForBackground = true
                        return@withLock
                    }
                    link.resumeRequest = null
                    // Not after a terminate that happened meanwhile.
                    if (request.generation >= generation.get() && link.session == null) {
                        startSession(link, connectionId, request.password, request.generation, reason = "after the live links")
                    }
                }
            }
        }
    }

    // The links that connected before (the connectors that are up) start first; the others once those are connected
    // again, or after FIRST_LINKS_HEAD_START at most. Starting every session at once makes each of them slower: a radio
    // just woken up carries all their relay connections, address lookups and handshakes together, most of them for
    // connectors that are not there.
    private suspend fun restartAfterBackground() {
        val (first, then) = links.entries.partition { (_, link) -> link.hasConnected }
        restartLinks(first)
        if (first.isNotEmpty() && then.isNotEmpty()) {
            val firstIds = first.map { (connectionId, _) -> connectionId.id }
            withTimeoutOrNull(FIRST_LINKS_HEAD_START) {
                status.first { statuses -> firstIds.all { id -> statuses[id] == PeerConnectionStatus.OPEN } }
            }
        }
        restartLinks(then)
        updateWatching()
    }

    private suspend fun restartLinks(toRestart: List<Map.Entry<ConnectionIdHolder, Link>>) = coroutineScope {
        toRestart.forEach { (connectionId, link) ->
            launch {
                link.mutex.withLock {
                    if (!link.isStoppedForBackground) return@withLock
                    link.isStoppedForBackground = false
                    val request = link.resumeRequest
                    // A link being linked starts when the linking ends.
                    if (link.pauseCount > 0 || request == null) return@withLock

                    link.resumeRequest = null
                    // Not after a terminate that happened meanwhile.
                    if (request.generation >= generation.get() && link.session == null) {
                        startSession(link, connectionId, request.password, request.generation, reason = REASON_APP_FOREGROUND)
                    }
                }
            }
        }
    }

    private fun link(connectionId: ConnectionIdHolder): Link = links.computeIfAbsent(connectionId) { Link() }

    private suspend fun send(session: Session, message: String): Result<Unit> = runCatchingCancellable {
        inFlight.track { withContext(ioDispatcher) { session.handle.send(message) } }
    }.onFailure { error ->
        val reason = when (error) {
            is MessageNotConfirmedException -> "not confirmed within the ${error.limitMs} ms limit (maybe delivered)"
            is MessageNotSentException -> "not sent (${error.cause?.javaClass?.simpleName ?: "no connection"})"
            else -> "${error.describe()} (maybe delivered)"
        }
        lastErrors[session.connectionId] = "send: $reason"
        events.record(session.connectionId, SOURCE, "a message failed: $reason")
        // The size only: never the contents.
        logDiagnostics(session, "failed to send a message (${message.length} chars): $reason", isFailure = true)
    }

    private suspend fun terminateLink(connectionId: ConnectionIdHolder, link: Link, terminatedGeneration: Long) {
        link.mutex.withLock {
            if ((link.resumeRequest?.generation ?: Long.MAX_VALUE) <= terminatedGeneration) {
                link.resumeRequest = null
            }
            link.session
                ?.takeIf { session -> session.generation <= terminatedGeneration }
                ?.let { session -> stopSession(link, session, reason = "every connection terminated") }
            if (link.session == null) {
                status.update { statuses -> statuses - connectionId.id }
            }
        }
    }

    private suspend fun pause(connectionId: ConnectionIdHolder) = withContext(ioDispatcher) {
        val link = link(connectionId)
        link.mutex.withLock {
            link.pauseCount++
            if (link.pauseCount > 1) {
                RadixConnectLog.w(
                    "🧊 ${connectionId.logId} is being linked ${link.pauseCount} times at once: the sessions compete for its key"
                )
            }
            link.session?.let { session ->
                if (session.generation >= generation.get()) {
                    link.resumeRequest = ResumeRequest(password = session.password, generation = session.generation)
                }
                stopSession(link, session, reason = "paused while the link is being linked (same endpoint key)")
                setStatus(connectionId, PeerConnectionStatus.CLOSED)
            }
        }
        updateWatching()
    }

    private suspend fun resume(connectionId: ConnectionIdHolder) = withContext(ioDispatcher) {
        val link = link(connectionId)
        link.mutex.withLock {
            link.pauseCount = (link.pauseCount - 1).coerceAtLeast(0)
            val request = link.resumeRequest
            // In the background, the session starts when the app comes back.
            if (link.pauseCount > 0 || request == null || link.isStoppedForBackground) return@withLock

            link.resumeRequest = null
            // Not after a terminate that happened during the pause.
            if (request.generation >= generation.get() && link.session == null) {
                startSession(link, connectionId, request.password, request.generation, reason = "resumed after the linking")
            }
        }
        updateWatching()
    }

    // Requires the link's mutex.
    @Suppress("LongParameterList")
    private suspend fun startSession(
        link: Link,
        connectionId: ConnectionIdHolder,
        password: RadixConnectPassword,
        sessionGeneration: Long,
        reason: String
    ): Result<Unit> {
        val (relayUrl, handle) = runCatchingCancellable {
            val relayUrl = currentRelayUrl()
            // What the link's previous session remembered: a connector out of reach for a long time is then dialed only
            // now and then (the library waits for its knock), instead of again and again.
            val linkMemory = runCatchingCancellable { settings.irohLinkMemory(connectionId) }.getOrNull()
            relayUrl to sessionFactory.startWalletSession(password = password, relayUrl = relayUrl, linkMemory = linkMemory)
        }.getOrElse { error ->
            lastErrors[connectionId] = "start: ${error.describe()}"
            events.record(connectionId, SOURCE, "session failed to start: ${error.describe()}")
            logThrottle.log(connectionId.id, "🧊 ${connectionId.logId} iroh session failed to start ($reason): ${error.describe()}") {
                RadixConnectLog.w(it)
            }
            return Result.failure(error)
        }

        val session = Session(
            connectionId = connectionId,
            password = password,
            relayUrl = relayUrl,
            generation = sessionGeneration,
            handle = handle
        )
        link.session = session
        sessions[connectionId] = session
        // A session started while the app is in the background (within its grace period) says so on connecting.
        if (appInBackground) runCatching { handle.setInBackground(true) }
        setStatus(connectionId, PeerConnectionStatus.CLOSED)
        session.eventsJob = applicationScope.launch(ioDispatcher) { readEvents(link, session) }
        val relay = relayUrl ?: "public relays"
        RadixConnectLog.i("🧊 ${connectionId.logId} iroh session started ($reason): relay=$relay, sessions running=${sessions.size}")
        events.record(connectionId, SOURCE, "session started ($reason)")
        return Result.success(Unit)
    }

    // Requires the link's mutex, held until the session is closed. Leaves the status of the connection to the caller.
    // [keepMemory]: false when the link is deleted (nothing is kept of it).
    private suspend fun stopSession(link: Link, session: Session, reason: String, keepMemory: Boolean = true) {
        // The last snapshot of the session, taken before it is closed.
        RadixConnectLog.i("🧊 ${session.connectionId.logId} iroh session stopping ($reason): ${session.summary()}")
        events.record(session.connectionId, SOURCE, "session stopped ($reason)")
        if (keepMemory) rememberLinkMemory(session)
        link.session = null
        sessions.remove(session.connectionId, session)
        session.eventsJob?.cancel()
        val startedAt = System.currentTimeMillis()
        release(session)
        RadixConnectLog.d("🧊 ${session.connectionId.logId} iroh session closed in ${System.currentTimeMillis() - startedAt} ms")
    }

    // Keeps what the session remembers of its link (when its connector was last connected, its relay) for the link's
    // next session. Best effort: without it the next session only dials as for a new link.
    private suspend fun rememberLinkMemory(session: Session) {
        runCatchingCancellable {
            val memory = session.handle.linkMemory()
            if (memory.isNotEmpty()) settings.rememberIrohLinkMemory(session.connectionId, memory)
        }.onFailure { error ->
            RadixConnectLog.d("🧊 ${session.connectionId.logId} could not keep the iroh link memory: ${error.describe()}")
        }
    }

    private suspend fun release(session: Session) = withContext(NonCancellable) {
        runCatchingCancellable { session.handle.shutdownAndClose() }
            .onFailure { error ->
                RadixConnectLog.w("🧊 ${session.connectionId.logId} failed to shut down the iroh session: ${error.describe()}")
            }
    }

    private suspend fun readEvents(link: Link, session: Session) {
        runCatchingCancellable { runEventLoop(session) }.onFailure { error ->
            RadixConnectLog.w("🧊 ${session.connectionId.logId} failed to read the iroh events: ${error.describe()}")
        }
        onSessionEnded(link, session)
    }

    // The only reader of the session's events, for its whole life. It does nothing slow: messages are handed over
    // to the collectors, waiting only for them to take each one.
    private suspend fun runEventLoop(session: Session) {
        var isRunning = true
        while (isRunning) {
            awaitCollector(session)
            when (val next = nextEventWhileCollected(session.handle)) {
                is NextEvent.Taken -> when (val event = next.event) {
                    is IrohEvent.StateChanged -> updateStatus(session, event.status)
                    is IrohEvent.Message -> deliver(session, event)
                }

                NextEvent.CollectorsGone -> Unit
                NextEvent.Ended -> isRunning = false
            }
        }
    }

    // While nobody collects the messages, nothing is taken from the session: its status is polled instead.
    private suspend fun awaitCollector(session: Session) {
        if (messages.subscriptionCount.value > 0) return

        coroutineScope {
            val statusPolling = launch {
                while (isActive) {
                    runCatchingCancellable { session.handle.status() }.onSuccess { updateStatus(session, it) }
                    delay(STATUS_POLL_INTERVAL)
                }
            }
            messages.subscriptionCount.first { count -> count > 0 }
            statusPolling.cancel()
        }
    }

    // Takes the next event, unless every collector goes away first: the cancelled call takes nothing.
    private suspend fun nextEventWhileCollected(handle: IrohSessionHandle): NextEvent = coroutineScope {
        val nextEvent = async { handle.nextEvent() }
        val collectorsWatch = launch {
            messages.subscriptionCount.first { count -> count == 0 }
            nextEvent.cancel()
        }
        try {
            nextEvent.await()?.let { event -> NextEvent.Taken(event) } ?: NextEvent.Ended
        } catch (expected: CancellationException) {
            // Rethrows when it is this coroutine that is cancelled.
            currentCoroutineContext().ensureActive()
            NextEvent.CollectorsGone
        } finally {
            collectorsWatch.cancel()
        }
    }

    // The message is confirmed already: queue it for the app, and take no further event until it is handed over (so
    // that the library holds back the next ones). It stays queued if the session stops meanwhile.
    private suspend fun deliver(session: Session, message: IrohEvent.Message) {
        val delivery = enqueue(
            DataChannelWrapperEvent.MessageFromRemoteConnectionId(
                connectionIdHolder = session.connectionId,
                messageInJsonString = message.message
            )
        )
        if (messages.subscriptionCount.value == 0) {
            RadixConnectLog.i(
                "🧊 ${session.connectionId.logId} holding a confirmed message (${message.message.length} chars) until the app listens"
            )
        }
        awaitCollector(session)
        delivery.delivered.await()
    }

    // The session ended without being stopped by this class.
    private suspend fun onSessionEnded(link: Link, session: Session) {
        link.mutex.withLock {
            if (link.session !== session) return@withLock

            RadixConnectLog.w("🧊 ${session.connectionId.logId} iroh session ended by itself: ${session.summary()}")
            events.record(session.connectionId, SOURCE, "session ended by itself")
            link.session = null
            sessions.remove(session.connectionId, session)
            setStatus(session.connectionId, PeerConnectionStatus.CLOSED)
            release(session)
        }
        updateWatching()
    }

    private fun updateStatus(session: Session, connectionStatus: IrohConnectionStatus) {
        val connectionId = session.connectionId.id
        val peerConnectionStatus = when (connectionStatus) {
            IrohConnectionStatus.CONNECTED -> PeerConnectionStatus.OPEN
            IrohConnectionStatus.CONNECTING, IrohConnectionStatus.DISCONNECTED -> PeerConnectionStatus.CLOSED
        }
        val previousStatus = status.value[connectionId]
        // Checked inside the atomic update: a session stopped concurrently (removed from `sessions` before its
        // status is changed) can never overwrite the status written for it.
        status.update { statuses ->
            if (sessions[session.connectionId] === session) statuses + (connectionId to peerConnectionStatus) else statuses
        }
        if (peerConnectionStatus == PeerConnectionStatus.OPEN) {
            links[session.connectionId]?.hasConnected = true
            // A link connected: the radio is awake, and a link connected from now on starts at once.
            coldStartOver = true
            // Kept at once: a process ended by the system stops no session, and the link must not look dormant next time.
            // Under the link's mutex, as deleting the link is: a memory is never written back after its link is deleted.
            applicationScope.launch(ioDispatcher) {
                links[session.connectionId]?.mutex?.withLock {
                    if (sessions[session.connectionId] === session) rememberLinkMemory(session)
                }
            }
        }
        val previousConnectionStatus = session.loggedConnectionStatus
        if (previousStatus != peerConnectionStatus || previousConnectionStatus != connectionStatus) {
            session.loggedConnectionStatus = connectionStatus
            val change = "${previousConnectionStatus?.name?.lowercase() ?: "started"} → ${connectionStatus.name.lowercase()}"
            // Every connect and disconnect is logged with the library's snapshot.
            logDiagnostics(session, "iroh $change", isFailure = false)
            events.record(session.connectionId, SOURCE, change)
        }
        if (connectionStatus == IrohConnectionStatus.CONNECTED) {
            foregroundSince.remove(session.connectionId)?.let { since ->
                logForegroundConnection(session.connectionId, "connected", since)
            }
        }
    }

    private fun logForegroundConnection(connectionId: ConnectionIdHolder, what: String, since: Long) {
        val elapsedMs = monotonicMillis() - since
        RadixConnectLog.i("🧊 ${connectionId.logId} iroh $what $elapsedMs ms after the app came to the foreground")
        events.record(connectionId, SOURCE, "$what $elapsedMs ms after the app came to the foreground")
    }

    // One line with the library's summary (short endpoint ids, relay, path, RTT, last error), collapsed when repeated.
    private fun logDiagnostics(session: Session, event: String, isFailure: Boolean) {
        logThrottle.log(session.connectionId.id, "🧊 ${session.connectionId.logId} $event: ${session.summary()}") { line ->
            if (isFailure) RadixConnectLog.w(line) else RadixConnectLog.i(line)
        }
    }

    // The library's one-line snapshot: no secrets, IP addresses removed.
    private fun Session.summary(): String = runCatchingCancellable { handle.diagnostics().summary }
        .getOrElse { error -> "no diagnostics (${error.javaClass.simpleName})" }

    private fun setStatus(connectionId: ConnectionIdHolder, peerConnectionStatus: PeerConnectionStatus) {
        status.update { statuses -> statuses + (connectionId.id to peerConnectionStatus) }
    }

    // Observes network and relay changes exactly while any session runs.
    private fun updateWatching() {
        synchronized(watchLock) {
            if (sessions.isEmpty()) {
                watchJob?.cancel()
                watchJob = null
            } else if (watchJob?.isActive != true) {
                watchJob = applicationScope.launch(ioDispatcher) { watchNetworkAndRelay() }
            }
        }
    }

    private suspend fun watchNetworkAndRelay() = coroutineScope {
        launch {
            val reasons = ConcurrentHashMap.newKeySet<String>()
            merge(networkChangeMonitor.changes().map { REASON_NETWORK_CHANGE }, connectionCheckRequests)
                .onEach { reason -> reasons += reason }
                .collectLatest {
                    // Network changes arrive in bursts (available, link properties...), often together with the app
                    // coming to the foreground: each session checks its connection once per burst.
                    delay(NETWORK_CHANGE_DEBOUNCE)
                    val batch = reasons.toSortedSet().also { taken -> reasons.removeAll(taken) }
                    // Not cut short by the next burst: that one waits for this check to end.
                    withContext(NonCancellable) { notifyNetworkChanged(batch) }
                }
        }
        launch {
            settings.irohRelayUrl
                .map { relayUrl -> sessionFactory.resolveRelayUrl(relayUrl) }
                .distinctUntilChanged()
                .collect { relayUrl -> applyRelayUrl(relayUrl) }
        }
    }

    // Tells every running session to check its connection (the library probes it and redials at once if it is dead).
    private suspend fun notifyNetworkChanged(reasons: Set<String>) {
        val currentSessions = sessions.values.toList()
        if (currentSessions.isEmpty()) return

        val why = reasons.joinToString()
        coroutineScope {
            currentSessions.forEach { session ->
                launch {
                    val connectionId = session.connectionId
                    // Collapsed when repeated: a flapping network changes often.
                    val line = "🧊 ${connectionId.logId} checking the iroh connection: $why"
                    logThrottle.log(connectionId.id + NETWORK_LOG_KEY_SUFFIX, line) { RadixConnectLog.i(it) }
                    events.record(connectionId, SOURCE, "connection check ($why)")
                    val startedAt = System.currentTimeMillis()
                    runCatchingCancellable { session.handle.networkChanged() }.onFailure { error ->
                        RadixConnectLog.w(
                            "🧊 ${connectionId.logId} the iroh connection check ($why) failed after " +
                                "${System.currentTimeMillis() - startedAt} ms: ${error.describe()}"
                        )
                    }
                    // Still connected after the check: the connection survived the background.
                    if (status.value[connectionId.id] == PeerConnectionStatus.OPEN) {
                        foregroundSince.remove(connectionId)?.let { since ->
                            logForegroundConnection(connectionId, "still connected", since)
                        }
                    }
                }
            }
        }
    }

    // Restarts the sessions that use another relay, link by link and concurrently. Not children of the watch job,
    // which ends when the last session stops.
    private fun applyRelayUrl(relayUrl: String?) {
        links.forEach { (connectionId, link) ->
            if (link.session?.let { session -> session.relayUrl != relayUrl } == true) {
                applicationScope.launch(ioDispatcher) { restartSession(connectionId, link, relayUrl) }
            }
        }
    }

    private suspend fun restartSession(connectionId: ConnectionIdHolder, link: Link, relayUrl: String?) {
        link.mutex.withLock {
            val session = link.session?.takeIf { it.relayUrl != relayUrl } ?: return@withLock

            stopSession(link, session, reason = "the relay changed")
            startSession(link, connectionId, session.password, session.generation, reason = "the relay changed").onFailure {
                setStatus(connectionId, PeerConnectionStatus.CLOSED)
            }
        }
        updateWatching()
    }

    private suspend fun currentRelayUrl(): String? = sessionFactory.resolveRelayUrl(settings.irohRelayUrl.first())

    companion object {
        private val NETWORK_CHANGE_DEBOUNCE = 500.milliseconds
        private val STATUS_POLL_INTERVAL = 1.seconds

        // How long the links that connected before have to connect again, alone, after the app came back.
        private val FIRST_LINKS_HEAD_START = 3.seconds
        private const val SOURCE = "iroh"
        private const val NETWORK_LOG_KEY_SUFFIX = "/network"
        private const val REASON_NETWORK_CHANGE = "network change"
        private const val REASON_APP_FOREGROUND = "app foreground"
        private const val REASON_APP_BACKGROUND = "app in background"
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
