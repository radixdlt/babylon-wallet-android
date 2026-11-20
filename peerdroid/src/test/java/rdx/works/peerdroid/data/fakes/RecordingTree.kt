package rdx.works.peerdroid.data.fakes

import android.util.Log
import org.junit.rules.ExternalResource
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Records what is logged through Timber while a test runs.
 */
class RecordingTree : ExternalResource() {

    data class Line(val priority: Int, val tag: String?, val message: String) {
        override fun toString(): String = "${levels[priority] ?: priority}/$tag: $message"
    }

    val lines: MutableList<Line> = CopyOnWriteArrayList()

    private val tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            lines += Line(priority, tag, message)
        }
    }

    val messages: List<String>
        get() = lines.map { it.message }

    /**
     * The lines containing [text], for assertions.
     */
    fun linesWith(text: String): List<Line> = lines.filter { text in it.message }

    override fun before() {
        Timber.plant(tree)
    }

    override fun after() {
        Timber.uproot(tree)
    }

    companion object {
        private val levels = mapOf(Log.VERBOSE to "V", Log.DEBUG to "D", Log.INFO to "I", Log.WARN to "W", Log.ERROR to "E")
    }
}
