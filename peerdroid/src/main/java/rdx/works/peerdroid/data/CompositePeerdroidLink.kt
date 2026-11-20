package rdx.works.peerdroid.data

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import rdx.works.peerdroid.data.RadixConnectLog.LINKING_TAG
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The linking was completed over another transport (or abandoned): the transport that reported it must close its
 * connection without completing the linking.
 */
internal class LinkingSupersededException(message: String) : IllegalStateException(message)

/**
 * The [PeerdroidLink] the app uses: links a connector extension over the transports of
 * [TransportPolicy.linkingTransports], concurrently.
 *
 * Which transport completes the linking (and is the one [sendMessage] uses) follows the [PeerdroidTransport] order of
 * preference, iroh first:
 * - iroh connecting first wins at once.
 * - WebRTC connecting first while iroh is still trying waits up to [PREFERRED_TRANSPORT_GRACE] for iroh: iroh wins if
 *   it connects meanwhile; WebRTC wins once the grace period is over, or as soon as the iroh attempt ends.
 * - A less preferred transport that loses is cancelled and closed. A preferred one still trying when a less preferred
 *   one wins (iroh, once WebRTC won after the grace period) is not cancelled: it goes on in [learningScope], within its
 *   own time limit, only to find out whether the connector extension supports it, and is closed once it connects. The
 *   linking does not wait for it. Otherwise a link whose iroh lost the race (a QR scanned before the extension's iroh
 *   was reachable) would never use iroh in the automatic mode, which only starts iroh for links known to support it.
 * - Only the winner calls [PeerdroidLink.ConnectionListener.completeLinking] of the caller, once: the link is saved
 *   exactly once.
 *
 * The linking fails only if every transport fails. When iroh connected (winning or not, during the linking or after
 * it) the link is remembered as supporting iroh.
 *
 * @param learningScope where a preferred transport that lost goes on trying after the linking (the app's scope).
 * @param monotonicMillis a monotonic clock in milliseconds, for the durations in the logs.
 */
internal class CompositePeerdroidLink(
    private val links: Map<PeerdroidTransport, Lazy<PeerdroidLink>>,
    private val settings: RadixConnectTransportSettings,
    private val learningScope: CoroutineScope,
    private val preferredTransportGrace: Duration = PREFERRED_TRANSPORT_GRACE,
    private val monotonicMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLI }
) : PeerdroidLink {

    private val winner = AtomicReference<PeerdroidLink?>(null)

    override suspend fun addConnection(
        p2pLink: P2pLink,
        p2pTransportProfile: P2pTransportProfile,
        connectionListener: PeerdroidLink.ConnectionListener
    ): Result<Unit> {
        winner.set(null)
        val connectionId = ConnectionIdHolder(p2pLink)
        val policy = TransportPolicy(
            mode = settings.transport.first(),
            irohCapableConnectionIds = settings.irohCapableConnectionIds.first()
        )
        val wasIrohCapable = connectionId.id in policy.irohCapableConnectionIds
        val transports = policy.linkingTransports().filter { it in links.keys }.sortedBy { it.ordinal }
        RadixConnectLog.i(
            "🔗 ${connectionId.logId} linking started: transports=${transports.map { it.logName }}, " +
                "mode=${policy.mode.name.lowercase()}, irohCapable=$wasIrohCapable",
            tag = LINKING_TAG
        )

        val race = LinkingRace(connectionId = connectionId, transports = transports)
        val result = try {
            race.run(
                onWon = { transport ->
                    // Before the link is saved, so that its first connection uses iroh right away.
                    if (transport == PeerdroidTransport.IROH) remember(connectionId, reason = "iroh won the linking")
                },
                onLearned = { transport ->
                    if (transport == PeerdroidTransport.IROH) remember(connectionId, reason = "iroh connected after the linking")
                },
                p2pLink = p2pLink,
                p2pTransportProfile = p2pTransportProfile,
                connectionListener = connectionListener
            )
        } catch (cancelled: CancellationException) {
            race.stopLearning(reason = "the linking was abandoned")
            throw cancelled
        }
        if (result.isFailure) race.stopLearning(reason = "the linking failed")

        val irohConnected = PeerdroidTransport.IROH in race.connectedTransports
        when {
            result.isSuccess && irohConnected -> remember(connectionId, reason = "iroh connected while linking")
            // Remembered when iroh won, but the link was not saved after all.
            result.isFailure && !wasIrohCapable && irohConnected -> forget(connectionId, reason = "the link was not saved")
        }
        return result
    }

    override suspend fun sendMessage(
        connectionId: String,
        message: PeerdroidLink.LinkClientExchangeInteraction
    ): Result<Unit> = winner.get()?.sendMessage(connectionId, message)
        ?: Result.failure(MessageNotSentException("no transport is linking ${ConnectionIdHolder(connectionId).logId}"))

    private suspend fun remember(connectionId: ConnectionIdHolder, reason: String) {
        runCatchingCancellable { settings.rememberIrohSupport(connectionId) }
            .onSuccess { RadixConnectLog.i("🔗 ${connectionId.logId} iroh support learned: $reason", tag = LINKING_TAG) }
            .onFailure { error ->
                RadixConnectLog.w(
                    "🔗 ${connectionId.logId} failed to remember iroh support: ${error.javaClass.simpleName}",
                    tag = LINKING_TAG
                )
            }
    }

    private suspend fun forget(connectionId: ConnectionIdHolder, reason: String) {
        runCatchingCancellable { settings.forgetLink(connectionId) }
            .onSuccess { RadixConnectLog.i("🔗 ${connectionId.logId} iroh support forgotten: $reason", tag = LINKING_TAG) }
            .onFailure { error ->
                RadixConnectLog.w(
                    "🔗 ${connectionId.logId} failed to forget iroh support: ${error.javaClass.simpleName}",
                    tag = LINKING_TAG
                )
            }
    }

    /**
     * One linking of [connectionId] over [transports], given in order of preference.
     */
    private inner class LinkingRace(
        private val connectionId: ConnectionIdHolder,
        private val transports: List<PeerdroidTransport>
    ) {

        val connectedTransports: MutableSet<PeerdroidTransport> = ConcurrentHashMap.newKeySet()

        // The transport that completes the linking, decided once; null when the linking was abandoned meanwhile.
        private val decision = CompletableDeferred<PeerdroidTransport?>()

        private val isCompletionStarted = AtomicBoolean(false)

        // Filled before any attempt starts, read-only afterwards.
        private val attempts = mutableMapOf<PeerdroidTransport, Deferred<Result<Unit>>>()
        private val startedAt = monotonicMillis()
        private lateinit var raceJob: Job

        // When a less preferred transport started waiting for the preferred ones, and which one.
        @Volatile
        private var gracePeriod: Pair<PeerdroidTransport, Long>? = null

        // Completed for a preferred transport that lost while still trying: it goes on, and the race no longer waits for
        // it (see the class documentation).
        private val learning = transports.associateWith { CompletableDeferred<Unit>() }

        // The transports whose attempt runs in learningScope, outside of the race: the race's end does not cancel them.
        private val runOutside = mutableSetOf<PeerdroidTransport>()

        private val elapsedMs: Long
            get() = monotonicMillis() - startedAt

        suspend fun run(
            onWon: suspend (PeerdroidTransport) -> Unit,
            onLearned: suspend (PeerdroidTransport) -> Unit,
            p2pLink: P2pLink,
            p2pTransportProfile: P2pTransportProfile,
            connectionListener: PeerdroidLink.ConnectionListener
        ): Result<Unit> = coroutineScope {
            raceJob = coroutineContext.job
            val leastPreferred = transports.maxOfOrNull { it.ordinal }
            transports.forEach { transport ->
                val link = links.getValue(transport).value
                val listener = object : PeerdroidLink.ConnectionListener {
                    override suspend fun completeLinking(connectionId: String): Result<Unit> =
                        onConnected(transport, link, onWon, connectionId, connectionListener)
                }
                // A transport that may lose to a less preferred one may go on after the race: it runs outside of it.
                val outside = transport.ordinal != leastPreferred
                if (outside) runOutside += transport
                val scope = if (outside) learningScope else this
                attempts[transport] = scope.async(start = CoroutineStart.LAZY) {
                    attempt(transport) { link.addConnection(p2pLink, p2pTransportProfile, listener) }
                }.also { attempt ->
                    attempt.invokeOnCompletion {
                        if (learning.getValue(transport).isCompleted && transport in connectedTransports) {
                            learningScope.launch { onLearned(transport) }
                        }
                    }
                }
            }
            // The attempts running outside of the race are not cancelled with it: when the caller gives up, this stops them
            // at once (a less preferred transport waiting for them in its grace period then gives up too).
            var isOver = false
            val abandonment = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!isOver) stopLearning(reason = "the linking was abandoned")
                }
            }
            attempts.values.forEach { attempt -> attempt.start() }

            val results = attempts.mapValues { (transport, attempt) -> attempt.awaitUnlessLearning(transport) }
            isOver = true
            abandonment.cancel()
            val winningTransport = decision.takeIf { it.isCompleted }?.getCompleted()
            winningTransport?.let { results[it] }
                ?: results.values.filterNotNull().firstOrNull { it.isSuccess }
                ?: results.values.filterNotNull().firstOrNull()?.also { logEveryTransportFailed(results) }
                ?: Result.failure(IllegalStateException("no transport could link the connector extension"))
        }

        private suspend fun attempt(transport: PeerdroidTransport, addConnection: suspend () -> Result<Unit>): Result<Unit> {
            RadixConnectLog.d("🔗 ${connectionId.logId} ${transport.logName} attempt started", tag = LINKING_TAG)
            val result = try {
                addConnection()
            } catch (exception: CancellationException) {
                RadixConnectLog.d("🔗 ${connectionId.logId} ${transport.logName} attempt cancelled after $elapsedMs ms", tag = LINKING_TAG)
                throw exception
            }
            val error = result.exceptionOrNull()
            when {
                error == null -> RadixConnectLog.d(
                    "🔗 ${connectionId.logId} ${transport.logName} attempt ended after $elapsedMs ms",
                    tag = LINKING_TAG
                )

                error is LinkingSupersededException -> RadixConnectLog.d(
                    "🔗 ${connectionId.logId} ${transport.logName} attempt closed after $elapsedMs ms: ${error.message}",
                    tag = LINKING_TAG
                )

                else -> RadixConnectLog.w(
                    "🔗 ${connectionId.logId} ${transport.logName} attempt failed after $elapsedMs ms: ${error.describe()}",
                    tag = LINKING_TAG
                )
            }
            return result
        }

        // Called by a transport once it is connected to the connector extension: completes the linking if this
        // transport wins, otherwise tells it to close.
        private suspend fun onConnected(
            transport: PeerdroidTransport,
            link: PeerdroidLink,
            onWon: suspend (PeerdroidTransport) -> Unit,
            connectionId: String,
            connectionListener: PeerdroidLink.ConnectionListener
        ): Result<Unit> {
            connectedTransports += transport
            RadixConnectLog.i("🔗 ${this.connectionId.logId} ${transport.logName} connected after $elapsedMs ms", tag = LINKING_TAG)
            val winningTransport = decide(transport)
            if (winningTransport != transport || !isCompletionStarted.compareAndSet(false, true)) {
                val reason = winningTransport?.let { "the linking was completed over ${it.logName}" } ?: "the linking was abandoned"
                return Result.failure(LinkingSupersededException(reason))
            }

            winner.set(link)
            attempts.filterKeys { it != transport }.forEach { (loser, attempt) ->
                when {
                    !attempt.isActive -> Unit
                    loser.ordinal < transport.ordinal -> {
                        RadixConnectLog.i(
                            "🔗 ${this.connectionId.logId} the ${loser.logName} attempt lost to ${transport.logName} and goes on " +
                                "after the linking, to find out whether the connector extension supports it",
                            tag = LINKING_TAG
                        )
                        learning.getValue(loser).complete(Unit)
                    }

                    else -> {
                        RadixConnectLog.d(
                            "🔗 ${this.connectionId.logId} cancelling the ${loser.logName} attempt: it lost to ${transport.logName}",
                            tag = LINKING_TAG
                        )
                        attempt.cancel()
                    }
                }
            }
            onWon(transport)
            return connectionListener.completeLinking(connectionId)
        }

        // Decides, once, which transport completes the linking now that [transport] connected.
        private suspend fun decide(transport: PeerdroidTransport): PeerdroidTransport? {
            if (!decision.isCompleted) {
                val preferredAttempts = attempts.filterKeys { it.ordinal < transport.ordinal }
                val runningPreferredAttempts = preferredAttempts.filterValues { it.isActive }
                val reason = when {
                    runningPreferredAttempts.isNotEmpty() -> awaitPreferred(transport, runningPreferredAttempts)
                    preferredAttempts.isNotEmpty() -> "the ${preferredAttempts.keys.map { it.logName }} attempt had ended"
                    else -> gracePeriod?.let { (waitingTransport, since) ->
                        "it connected ${monotonicMillis() - since} ms into the ${preferredTransportGrace.inWholeMilliseconds} ms " +
                            "grace period given to it by ${waitingTransport.logName}"
                    } ?: "it connected first"
                }
                if (!raceJob.isActive) {
                    decision.complete(null)
                } else if (decision.complete(transport)) {
                    RadixConnectLog.i(
                        "🔗 ${connectionId.logId} linking won by ${transport.logName} after $elapsedMs ms: $reason",
                        tag = LINKING_TAG
                    )
                }
            }
            return decision.await()
        }

        // Waits up to the grace period for a preferred transport to win. Returns why [transport] wins if none did.
        private suspend fun awaitPreferred(
            transport: PeerdroidTransport,
            preferredAttempts: Map<PeerdroidTransport, Deferred<Result<Unit>>>
        ): String {
            val preferred = preferredAttempts.keys.map { it.logName }
            val graceMs = preferredTransportGrace.inWholeMilliseconds
            RadixConnectLog.i(
                "🔗 ${connectionId.logId} ${transport.logName} connected first, waiting up to $graceMs ms for $preferred",
                tag = LINKING_TAG
            )
            val waitStartedAt = monotonicMillis()
            gracePeriod = transport to waitStartedAt
            val isGraceOver = withTimeoutOrNull(preferredTransportGrace) {
                coroutineScope {
                    val preferredEnded = async { preferredAttempts.values.joinAll() }
                    select {
                        decision.onAwait {}
                        preferredEnded.onAwait {}
                    }
                    preferredEnded.cancel()
                }
            } == null
            val waitedMs = monotonicMillis() - waitStartedAt
            return when {
                decision.isCompleted -> "${decision.getCompleted()?.logName} connected $waitedMs ms into the grace period"
                isGraceOver -> "$preferred did not connect within the $graceMs ms grace period"
                else -> "the $preferred attempt ended $waitedMs ms into the $graceMs ms grace period"
            }
        }

        private fun logEveryTransportFailed(results: Map<PeerdroidTransport, Result<Unit>?>) {
            RadixConnectLog.e(
                "🔗 ${connectionId.logId} linking failed on every transport after $elapsedMs ms: " +
                    results.entries.joinToString { (transport, result) ->
                        "${transport.logName}=${result?.exceptionOrNull()?.describe() ?: "cancelled"}"
                    },
                tag = LINKING_TAG
            )
        }

        // Cancels the attempts running outside of the race (still in it, or going on after it): the linking failed or was
        // abandoned, there is nothing to complete nor to learn for.
        fun stopLearning(reason: String) {
            runOutside.forEach { transport ->
                val attempt = attempts[transport] ?: return@forEach
                if (attempt.isActive) {
                    RadixConnectLog.d("🔗 ${connectionId.logId} cancelling the ${transport.logName} attempt: $reason", tag = LINKING_TAG)
                    attempt.cancel()
                }
            }
        }

        // The attempt's result; null when it was cancelled because another transport won, or when it lost while still
        // trying and goes on after the linking (it is not waited for).
        private suspend fun Deferred<Result<Unit>>.awaitUnlessLearning(transport: PeerdroidTransport): Result<Unit>? = try {
            select {
                onAwait { it }
                learning.getValue(transport).onAwait { null }
            }
        } catch (expected: CancellationException) {
            // Rethrows if it is the caller that was cancelled.
            currentCoroutineContext().ensureActive()
            null
        }
    }

    companion object {
        /**
         * How long the linking waits for iroh when WebRTC connected first. iroh is the preferred transport (the link
         * is then remembered as supporting it, and it gets through where WebRTC cannot), and it usually connects
         * within a couple of seconds of WebRTC.
         */
        val PREFERRED_TRANSPORT_GRACE: Duration = 5.seconds

        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
