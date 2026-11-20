package rdx.works.peerdroid.data

import rdx.works.peerdroid.domain.ConnectionIdHolder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Spots a message of a link that arrives again, over the same transport or the other one.
 *
 * The connector extension sends a message again when it did not get its confirmation in time (10 s on both
 * transports), possibly over the other transport, although the wallet may have received it: the same interaction
 * then arrives twice. Only identical contents within [windowMs] count as the same message, so that a dApp asking
 * again later is not affected.
 *
 * @param monotonicMillis a monotonic clock in milliseconds, injectable for tests.
 */
internal class IncomingMessageDeduplicator(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val capacity: Int = DEFAULT_CAPACITY,
    private val monotonicMillis: () -> Long
) {

    /**
     * When and over which transport a message first arrived.
     */
    class Sighting(val transport: PeerdroidTransport, val atMillis: Long)

    private class Entry(val digest: String, val sighting: Sighting)

    // Immutable lists, replaced atomically.
    private val recent = ConcurrentHashMap<String, List<Entry>>()

    /**
     * Returns the earlier arrival of the same [message] on [connectionId] within the window, or null after
     * remembering this one.
     */
    fun previousSighting(connectionId: ConnectionIdHolder, message: String, transport: PeerdroidTransport): Sighting? {
        val digest = digestOf(message)
        val now = monotonicMillis()
        var previous: Sighting? = null
        recent.compute(connectionId.id) { _, entries ->
            val fresh = entries.orEmpty().filter { now - it.sighting.atMillis < windowMs }
            previous = fresh.firstOrNull { it.digest == digest }?.sighting
            if (previous != null) fresh else (fresh + Entry(digest, Sighting(transport, now))).takeLast(capacity)
        }
        return previous
    }

    fun forget(connectionId: ConnectionIdHolder) {
        recent.remove(connectionId.id)
    }

    private fun digestOf(message: String): String = MessageDigest.getInstance(DIGEST_ALGORITHM)
        .digest(message.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        const val DEFAULT_WINDOW_MS = 120_000L
        private const val DEFAULT_CAPACITY = 32
        private const val DIGEST_ALGORITHM = "SHA-256"
    }
}
