package rdx.works.peerdroid.data.iroh

import android.util.Log
import com.radixdlt.connect.iroh.IrohLogLevel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import rdx.works.peerdroid.data.fakes.RecordingTree

class TimberIrohLogSinkTest {

    @get:Rule
    val logs = RecordingTree()

    @Test
    fun `the library's lines keep their level, under their own tag`() {
        TimberIrohLogSink.log(IrohLogLevel.ERROR, "radix_connect_iroh::levels", "error")
        TimberIrohLogSink.log(IrohLogLevel.WARN, "radix_connect_iroh::levels", "warning")
        TimberIrohLogSink.log(IrohLogLevel.INFO, "radix_connect_iroh::levels", "info")
        TimberIrohLogSink.log(IrohLogLevel.DEBUG, "radix_connect_iroh::levels", "debug")
        TimberIrohLogSink.log(IrohLogLevel.TRACE, "radix_connect_iroh::levels", "trace")

        assertEquals(
            listOf(
                RecordingTree.Line(Log.ERROR, IROH_LIBRARY_LOG_TAG, "radix_connect_iroh::levels: error"),
                RecordingTree.Line(Log.WARN, IROH_LIBRARY_LOG_TAG, "radix_connect_iroh::levels: warning"),
                RecordingTree.Line(Log.INFO, IROH_LIBRARY_LOG_TAG, "radix_connect_iroh::levels: info"),
                RecordingTree.Line(Log.DEBUG, IROH_LIBRARY_LOG_TAG, "radix_connect_iroh::levels: debug"),
                RecordingTree.Line(Log.VERBOSE, IROH_LIBRARY_LOG_TAG, "radix_connect_iroh::levels: trace")
            ),
            logs.lines
        )
    }

    @Test
    fun `a warning that another holder of the link's key took the link over is kept, without IP addresses`() {
        TimberIrohLogSink.log(
            IrohLogLevel.WARN,
            "radix_connect_iroh::session",
            "another session holding this link's key took the link over (from 198.51.100.4:51820)"
        )

        assertEquals(
            listOf(
                RecordingTree.Line(
                    Log.WARN,
                    IROH_LIBRARY_LOG_TAG,
                    "radix_connect_iroh::session: another session holding this link's key took the link over (from <ip>)"
                )
            ),
            logs.lines
        )
    }

    @Test
    fun `identical lines repeated by the same target are collapsed`() {
        repeat(3) { TimberIrohLogSink.log(IrohLogLevel.WARN, "iroh::relay", "relay unreachable") }
        TimberIrohLogSink.log(IrohLogLevel.WARN, "iroh::relay", "relay reachable again")

        assertEquals(
            listOf(
                "iroh::relay: relay unreachable",
                "iroh::relay: relay unreachable (repeated 2 more times)",
                "iroh::relay: relay reachable again"
            ),
            logs.messages
        )
    }
}
