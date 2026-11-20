package rdx.works.peerdroid.data.iroh

import com.radixdlt.sargon.P2pLink
import com.radixdlt.sargon.P2pTransportProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import rdx.works.peerdroid.data.LinkingSupersededException
import rdx.works.peerdroid.data.PeerdroidLink
import rdx.works.peerdroid.data.RadixConnectLog
import rdx.works.peerdroid.data.RadixConnectLog.LINKING_TAG
import rdx.works.peerdroid.data.RadixConnectTransportSettings
import rdx.works.peerdroid.data.describe
import rdx.works.peerdroid.data.logId
import rdx.works.peerdroid.data.runCatchingCancellable
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Links a connector extension over iroh: starts a wallet session for the scanned password, waits until the
 * extension's pairing popup (which runs the connector end of the same link) is connected, and lets the caller send
 * the `linkClient` message through [sendMessage]. The linking is over as soon as it completes or fails; the session
 * is then closed in [closingScope], since closing waits for its connection to drain (about three round trips), which
 * nobody has to wait for. The regular session of the link stays paused until that close is done (same endpoint key).
 */
internal class IrohPeerdroidLink(
    private val sessionFactory: IrohSessionFactory,
    private val irohConnector: IrohConnector,
    private val settings: RadixConnectTransportSettings,
    private val ioDispatcher: CoroutineDispatcher,
    private val closingScope: CoroutineScope,
    private val connectTimeout: Duration = CONNECT_TIMEOUT
) : PeerdroidLink {

    private class LinkingSession(val connectionId: String, val handle: IrohSessionHandle) {
        val startedAt: Long = System.currentTimeMillis()

        val logId: String
            get() = ConnectionIdHolder(connectionId).logId

        val elapsedMs: Long
            get() = System.currentTimeMillis() - startedAt
    }

    private val linkingSession = AtomicReference<LinkingSession?>(null)

    override suspend fun addConnection(
        p2pLink: P2pLink,
        p2pTransportProfile: P2pTransportProfile,
        connectionListener: PeerdroidLink.ConnectionListener
    ): Result<Unit> {
        val connectionId = ConnectionIdHolder(p2pLink)
        RadixConnectLog.d("🧊 ${connectionId.logId} start linking over iroh", tag = LINKING_TAG)
        val outcome = CompletableDeferred<Result<Unit>>()
        // The linking session has the same endpoint key as the regular session of this link (when re-linking an
        // existing connector, or when the link is saved before this session is closed): keep that one stopped until
        // the linking session is closed, which outlasts the linking itself.
        val linking = closingScope.launch(ioDispatcher) {
            irohConnector.withSessionPaused(connectionId) {
                link(p2pLink = p2pLink, connectionId = connectionId, connectionListener = connectionListener, onOutcome = {
                    outcome.complete(it)
                })
            }
        }
        // No outcome if the linking ended otherwise (cancelled, or a failure of the pause itself).
        linking.invokeOnCompletion { cause ->
            outcome.complete(Result.failure(cause ?: IllegalStateException("the iroh linking ended without an outcome")))
        }
        return try {
            outcome.await()
        } catch (cancelled: CancellationException) {
            linking.cancel()
            throw cancelled
        }
    }

    override suspend fun sendMessage(
        connectionId: String,
        message: PeerdroidLink.LinkClientExchangeInteraction
    ): Result<Unit> {
        val session = linkingSession.get()?.takeIf { it.connectionId == connectionId }
            ?: return Result.failure(MessageNotSentException("no iroh linking session for connector $connectionId"))

        RadixConnectLog.d("🧊 ${session.logId} sending the link client message over iroh", tag = LINKING_TAG)
        return runCatchingCancellable { session.handle.send(Json.encodeToString(message)) }.onSuccess {
            RadixConnectLog.i(
                "🧊 ${session.logId} link client message confirmed over iroh after ${session.elapsedMs} ms",
                tag = LINKING_TAG
            )
        }.onFailure { error ->
            RadixConnectLog.w(
                "🧊 ${session.logId} failed to send the link client message (${error.describe()}): ${session.summary()}",
                tag = LINKING_TAG
            )
        }
    }

    private suspend fun link(
        p2pLink: P2pLink,
        connectionId: ConnectionIdHolder,
        connectionListener: PeerdroidLink.ConnectionListener,
        onOutcome: (Result<Unit>) -> Unit
    ) {
        val handle = runCatchingCancellable {
            val relayUrl = sessionFactory.resolveRelayUrl(settings.irohRelayUrl.first())
            sessionFactory.startWalletSession(password = p2pLink.connectionPassword, relayUrl = relayUrl)
        }.getOrElse { error ->
            RadixConnectLog.w("🧊 ${connectionId.logId} failed to start the iroh linking session: ${error.describe()}", tag = LINKING_TAG)
            onOutcome(Result.failure(error))
            return
        }

        completeLinking(
            session = LinkingSession(connectionId = connectionId.id, handle = handle),
            connectionListener = connectionListener,
            onOutcome = onOutcome
        )
    }

    private suspend fun completeLinking(
        session: LinkingSession,
        connectionListener: PeerdroidLink.ConnectionListener,
        onOutcome: (Result<Unit>) -> Unit
    ): Unit = coroutineScope {
        val isConnected = CompletableDeferred<Boolean>()
        // The one reader of the session's events, until the session is shut down.
        launch { readEvents(session, isConnected) }
        try {
            val result = completeLinkingOnceConnected(session, isConnected, connectionListener)
            linkingSession.compareAndSet(session, null)
            onOutcome(result)
        } finally {
            linkingSession.compareAndSet(session, null)
            shutDown(session.handle)
        }
    }

    private suspend fun completeLinkingOnceConnected(
        session: LinkingSession,
        isConnected: Deferred<Boolean>,
        connectionListener: PeerdroidLink.ConnectionListener
    ): Result<Unit> {
        val limitMs = connectTimeout.inWholeMilliseconds
        when (withTimeoutOrNull(connectTimeout) { isConnected.await() }) {
            true -> Unit
            false -> {
                RadixConnectLog.w(
                    "🧊 ${session.logId} the iroh linking session ended after ${session.elapsedMs} ms: ${session.summary()}",
                    tag = LINKING_TAG
                )
                return Result.failure(IllegalStateException("the iroh linking session ended before connecting"))
            }

            null -> {
                RadixConnectLog.w(
                    "🧊 ${session.logId} could not reach the connector extension over iroh within the $limitMs ms limit: " +
                        session.summary(),
                    tag = LINKING_TAG
                )
                return Result.failure(TimeoutException("could not reach the connector extension over iroh within $limitMs ms"))
            }
        }

        RadixConnectLog.i(
            "🧊 ${session.logId} connected to the connector extension over iroh after ${session.elapsedMs} ms: ${session.summary()}",
            tag = LINKING_TAG
        )
        linkingSession.set(session)
        return connectionListener.completeLinking(session.connectionId).onFailure { error ->
            if (error is LinkingSupersededException) {
                RadixConnectLog.d("🧊 ${session.logId} closing the iroh linking session: ${error.message}", tag = LINKING_TAG)
            } else {
                RadixConnectLog.w(
                    "🧊 ${session.logId} failed to complete the linking over iroh (${error.describe()}): ${session.summary()}",
                    tag = LINKING_TAG
                )
            }
        }
    }

    // The library's one-line snapshot (short endpoint ids, relay, last error...): no secrets, IP addresses removed.
    private fun LinkingSession.summary(): String = runCatchingCancellable { handle.diagnostics().summary }
        .getOrElse { error -> "no diagnostics (${error.javaClass.simpleName})" }

    // Completes [isConnected] with true once connected, or with false if the session ends first. A message taken here
    // is confirmed to the connector extension (e.g. a dApp request sent right after the linking, on this connection):
    // it goes to the incoming messages of the link like any other, never dropped.
    private suspend fun readEvents(session: LinkingSession, isConnected: CompletableDeferred<Boolean>) {
        runCatchingCancellable {
            var event = session.handle.nextEvent()
            while (event != null) {
                when (event) {
                    is IrohEvent.StateChanged -> if (event.status == IrohConnectionStatus.CONNECTED) isConnected.complete(true)
                    is IrohEvent.Message -> irohConnector.deliverReceivedMessage(
                        connectionId = ConnectionIdHolder(session.connectionId),
                        message = event.message
                    )
                }
                event = session.handle.nextEvent()
            }
        }
        isConnected.complete(false)
    }

    private suspend fun shutDown(handle: IrohSessionHandle) {
        withContext(NonCancellable) {
            runCatchingCancellable { handle.shutdownAndClose() }
        }
    }

    companion object {
        // Covers a few dial attempts of the session (each one may take up to 20 s).
        private val CONNECT_TIMEOUT = 60.seconds
    }
}
