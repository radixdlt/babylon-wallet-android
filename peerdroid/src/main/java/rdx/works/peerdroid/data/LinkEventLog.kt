package rdx.works.peerdroid.data

import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.LinkEvent
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps the last few [LinkEvent]s of each link, for the diagnostics report. An event that repeats the previous one of
 * its link (same source and description) is counted into it instead of being added again.
 *
 * Descriptions follow the rules of [RadixConnectLog]: no secrets, no message contents, no IP addresses.
 *
 * @param clock milliseconds since the epoch, injectable for tests.
 */
internal class LinkEventLog(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val clock: () -> Long = System::currentTimeMillis
) {

    // Immutable lists, replaced atomically: reads need no locking.
    private val events = ConcurrentHashMap<String, List<LinkEvent>>()

    fun record(connectionId: ConnectionIdHolder, source: String, description: String) {
        val now = clock()
        events.compute(connectionId.id) { _, previousEvents ->
            val existing = previousEvents.orEmpty()
            val last = existing.lastOrNull()
            if (last != null && last.source == source && last.description == description) {
                existing.dropLast(1) + last.copy(atMillis = now, count = last.count + 1)
            } else {
                (existing + LinkEvent(atMillis = now, source = source, description = description)).takeLast(capacity)
            }
        }
    }

    fun recent(connectionId: ConnectionIdHolder): List<LinkEvent> = events[connectionId.id].orEmpty()

    /**
     * Forgets the events of [connectionId], once the link is deleted.
     */
    fun forget(connectionId: ConnectionIdHolder) {
        events.remove(connectionId.id)
    }

    companion object {
        private const val DEFAULT_CAPACITY = 16
    }
}
