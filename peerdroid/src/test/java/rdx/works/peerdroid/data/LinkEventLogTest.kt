package rdx.works.peerdroid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.LinkEvent

class LinkEventLogTest {

    private var now = 1_000L
    private val sut = LinkEventLog(capacity = 3, clock = { now })
    private val linkA = ConnectionIdHolder("a".repeat(64))
    private val linkB = ConnectionIdHolder("b".repeat(64))

    @Test
    fun `events are kept per link, oldest first, up to the capacity`() {
        listOf("one", "two", "three", "four").forEach { description ->
            now += 10
            sut.record(linkA, "iroh", description)
        }
        sut.record(linkB, "webrtc", "other link")

        assertEquals(listOf("two", "three", "four"), sut.recent(linkA).map { it.description })
        assertEquals(listOf("other link"), sut.recent(linkB).map { it.description })
    }

    @Test
    fun `an event repeating the previous one is counted into it`() {
        sut.record(linkA, "iroh", "session failed to start: no network")
        now = 2_000
        sut.record(linkA, "iroh", "session failed to start: no network")
        sut.record(linkA, "webrtc", "session failed to start: no network")

        assertEquals(
            listOf(
                LinkEvent(atMillis = 2_000, source = "iroh", description = "session failed to start: no network", count = 2),
                LinkEvent(atMillis = 2_000, source = "webrtc", description = "session failed to start: no network")
            ),
            sut.recent(linkA)
        )
    }

    @Test
    fun `a deleted link's events are forgotten`() {
        sut.record(linkA, "link", "connecting")

        sut.forget(linkA)

        assertTrue(sut.recent(linkA).isEmpty())
    }
}
