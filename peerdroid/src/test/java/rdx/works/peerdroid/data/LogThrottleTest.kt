package rdx.works.peerdroid.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LogThrottleTest {

    private var now = 0L
    private val logged = mutableListOf<String>()
    private val sut = LogThrottle(windowMs = 60_000, clock = { now })

    private fun log(key: String, line: String) = sut.log(key, line) { logged += it }

    @Test
    fun `repeated identical lines are logged once per window, with the count of the skipped ones`() {
        log("a", "dial timed out")
        log("a", "dial timed out")
        log("a", "dial timed out")
        now = 61_000
        log("a", "dial timed out")

        assertEquals(listOf("dial timed out", "dial timed out (repeated 2 more times)"), logged)
    }

    @Test
    fun `the skipped repetitions are reported before a different line`() {
        log("a", "dial timed out")
        log("a", "dial timed out")
        log("a", "dial timed out")
        log("a", "connected")

        assertEquals(listOf("dial timed out", "dial timed out (repeated 2 more times)", "connected"), logged)
    }

    @Test
    fun `a different line is logged at once`() {
        log("a", "dial timed out")
        log("a", "connected")
        log("a", "dial timed out")

        assertEquals(listOf("dial timed out", "connected", "dial timed out"), logged)
    }

    @Test
    fun `keys are throttled independently`() {
        log("a", "dial timed out")
        log("b", "dial timed out")

        assertEquals(listOf("dial timed out", "dial timed out"), logged)
    }

    @Test
    fun `a forgotten key logs again at once`() {
        log("a", "dial timed out")
        sut.forget("a")
        log("a", "dial timed out")

        assertEquals(listOf("dial timed out", "dial timed out"), logged)
    }
}
