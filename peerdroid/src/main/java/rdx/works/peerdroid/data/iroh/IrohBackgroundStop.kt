package rdx.works.peerdroid.data.iroh

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rdx.works.peerdroid.data.RadixConnectLog
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The exchanges with connector extensions in progress (a message being sent, or confirmed already and waiting to be
 * handed to the app), so that stopping the sessions does not cut one.
 */
internal class InFlightExchanges {

    private val count = MutableStateFlow(0)

    val current: Int
        get() = count.value

    fun begin() = count.update { it + 1 }

    fun end() = count.update { it - 1 }

    suspend fun <T> track(block: suspend () -> T): T {
        begin()
        try {
            return block()
        } finally {
            end()
        }
    }

    /**
     * Waits until no exchange is in progress, for at most [limit]. Returns how many are still in progress.
     */
    suspend fun awaitNone(limit: Duration): Int = withTimeoutOrNull(limit) { count.first { it <= 0 } } ?: count.value
}

/**
 * Stops the iroh sessions while the app is in the background, where requests cannot be approved anyway: offline
 * connectors are not dialed again and again, and connected ones get no keep-alives, until the app comes back (it then
 * reconnects in well under a second).
 *
 * - [onAppBackground] stops them after [grace], so that a quick switch to another app drops nothing, and once the
 *   exchanges in progress are over (waiting at most [inFlightLimit] more).
 * - [onAppForeground] cancels a pending stop, or restarts the sessions stopped.
 *
 * The stop and the restart are serialised: a restart always follows the stop it undoes.
 *
 * @param stopSessions stops every session; called with [isStopped] already true.
 * @param restartSessions restarts the sessions stopped; called with [isStopped] already false.
 */
@Suppress("LongParameterList")
internal class IrohBackgroundStop(
    private val applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val inFlight: InFlightExchanges,
    private val stopSessions: suspend () -> Unit,
    private val restartSessions: suspend () -> Unit,
    private val grace: Duration = BACKGROUND_GRACE,
    private val inFlightLimit: Duration = IN_FLIGHT_LIMIT
) {

    private val lock = Any()

    // Guarded by lock.
    private var pendingStop: Job? = null

    // Serialises the stop and the restart.
    private val mutex = Mutex()

    // Incremented by every background and foreground: a stop goes ahead only if nothing happened since it was asked.
    private val lifecycleGeneration = AtomicLong(0)

    /**
     * Whether the sessions are stopped (or being stopped) because the app is in the background: a session asked for
     * meanwhile starts only once the app comes back.
     */
    @Volatile
    var isStopped: Boolean = false
        private set

    fun onAppBackground() {
        synchronized(lock) {
            if (pendingStop?.isActive == true) return
            val requestGeneration = lifecycleGeneration.incrementAndGet()
            RadixConnectLog.i(
                "🧊 app in the background: stopping the iroh sessions in ${grace.inWholeMilliseconds} ms unless it comes back"
            )
            pendingStop = applicationScope.launch(ioDispatcher) { stopAfterGrace(requestGeneration) }
        }
    }

    fun onAppForeground() {
        val wasStopPending = synchronized(lock) {
            lifecycleGeneration.incrementAndGet()
            val isPending = pendingStop?.isActive == true
            pendingStop?.cancel()
            pendingStop = null
            isPending
        }
        if (wasStopPending) {
            RadixConnectLog.i("🧊 app back in the foreground within the grace period: the iroh sessions are kept")
        }
        applicationScope.launch(ioDispatcher) {
            mutex.withLock {
                if (!isStopped) return@withLock
                isStopped = false
                RadixConnectLog.i("🧊 app in the foreground: restarting the iroh sessions stopped in the background")
                restartSessions()
            }
        }
    }

    private suspend fun stopAfterGrace(requestGeneration: Long) {
        delay(grace)
        if (inFlight.current > 0) {
            RadixConnectLog.i(
                "🧊 waiting up to ${inFlightLimit.inWholeMilliseconds} ms for ${inFlight.current} exchange(s) in progress " +
                    "before stopping the iroh sessions"
            )
            val stillInProgress = inFlight.awaitNone(inFlightLimit)
            if (stillInProgress > 0) {
                RadixConnectLog.w(
                    "🧊 stopping the iroh sessions with $stillInProgress exchange(s) still in progress after the " +
                        "${inFlightLimit.inWholeMilliseconds} ms limit"
                )
            }
        }
        mutex.withLock {
            // The app came back meanwhile.
            if (lifecycleGeneration.get() != requestGeneration) return@withLock
            withContext(NonCancellable) {
                isStopped = true
                stopSessions()
            }
        }
    }

    companion object {
        /**
         * How long the app may stay in the background before the sessions stop: a quick look at another app drops
         * nothing.
         */
        val BACKGROUND_GRACE: Duration = 30.seconds

        /**
         * How much longer the stop waits for the exchanges in progress.
         */
        val IN_FLIGHT_LIMIT: Duration = 10.seconds
    }
}
