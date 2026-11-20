package rdx.works.peerdroid.data.iroh

import com.radixdlt.sargon.RadixConnectPassword
import rdx.works.peerdroid.data.RadixConnectLog
import rdx.works.peerdroid.data.runCatchingCancellable
import rdx.works.peerdroid.domain.IrohSessionDiagnostics
import rdx.works.peerdroid.domain.MessageNotSentException

/**
 * Relay settings of the iroh library. The only iroh type the app sees (for the debug settings).
 */
interface IrohRelayConfig {

    /**
     * n0's public relays, used when no custom relay is configured (each end uses its closest one): for display.
     */
    fun publicRelayUrls(): List<String>

    /**
     * Returns null when [relayUrl] is usable, otherwise why it is not.
     */
    fun validateRelayUrl(relayUrl: String): String?
}

/**
 * Starts iroh sessions. The only seam between peerdroid and the native iroh library, so that everything built on
 * top of it can be tested without native code.
 */
internal interface IrohSessionFactory : IrohRelayConfig {

    /**
     * Starts the wallet end of the link secured by [password], through the custom relay [relayUrl], or through n0's
     * public relays when null (the connector extension is then found through n0's address lookup). The session
     * connects (and reconnects) in the background; it is not connected yet when this returns.
     */
    suspend fun startWalletSession(
        password: RadixConnectPassword,
        relayUrl: String?,
        linkMemory: String? = null
    ): IrohSessionHandle

    /**
     * Whether the link whose memory is [linkMemory] was connected in the last day, as the library reads it (an
     * unreadable memory counts as connected lately).
     */
    fun connectedLately(linkMemory: String): Boolean
}

/**
 * One end of a Radix Connect link over iroh.
 */
internal interface IrohSessionHandle {

    /**
     * Waits for the next event, or returns null once the session has been shut down.
     *
     * Taking a message from here confirms it to the peer, so only call it when the message can be handed over.
     * Cancel-safe: a cancelled call neither loses nor confirms a message.
     */
    suspend fun nextEvent(): IrohEvent?

    /**
     * The current connection status, without taking any event.
     */
    fun status(): IrohConnectionStatus

    /**
     * A connectivity snapshot (no secrets), for logs and diagnostics.
     */
    fun diagnostics(): IrohSessionDiagnostics

    /**
     * Sends [message] and waits until the peer confirmed it.
     *
     * @throws MessageNotSentException when nothing was sent (not connected, or shut down) and the message may be
     * sent over another transport.
     * @throws MessageNotConfirmedException when the peer did not confirm it in time. This and any other exception
     * mean the message may have been delivered: it is not sent over another transport.
     */
    suspend fun send(message: String)

    /**
     * Tells the session the device's network changed.
     */
    suspend fun networkChanged()

    /**
     * Tells the connector whether the app is in the background, now and on every later connection: a connector
     * racing several wallets for one request then sends it to the one the person opens, not to this one while it
     * is only still connected after the app went to the background. Best effort.
     */
    fun setInBackground(inBackground: Boolean)

    /**
     * Closes the connection (telling the peer) and frees the native resources. Idempotent.
     */
    suspend fun shutdownAndClose()

    /**
     * What the session remembers of its link (opaque, no secret): kept with the link and given back to
     * [IrohSessionFactory.startWalletSession] for its next session.
     */
    suspend fun linkMemory(): String
}

/**
 * The connector extension did not confirm a message within [limitMs]: it may have received it, so the message must
 * not be sent again over another transport.
 */
internal class MessageNotConfirmedException(
    val limitMs: Long,
    cause: Throwable? = null
) : Exception("the connector extension did not confirm the message within $limitMs ms", cause)

internal enum class IrohConnectionStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

internal sealed interface IrohEvent {

    data class StateChanged(val status: IrohConnectionStatus) : IrohEvent

    class Message(val message: String) : IrohEvent {
        // Never expose the message contents through logs.
        override fun toString(): String = "Message(length=${message.length})"
    }
}

/**
 * The custom relay URL a session should use for the stored [relayUrl]: null (n0's public relays) when nothing usable
 * is stored, or when it cannot be validated. The settings UI only stores valid URLs; this guards against anything else.
 */
internal fun IrohRelayConfig.resolveRelayUrl(relayUrl: String?): String? {
    val candidate = relayUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val error = runCatchingCancellable { validateRelayUrl(candidate) }
        .getOrElse { exception -> "it could not be validated (${exception.javaClass.simpleName})" }
        ?: return candidate
    RadixConnectLog.w("🧊 ignoring the configured iroh relay ($error), using the public relays")
    return null
}
