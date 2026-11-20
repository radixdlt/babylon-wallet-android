package rdx.works.peerdroid.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps repeated identical log lines from flooding the logs: a line is logged when it differs from the previous one
 * for the same key, or when [windowMs] passed since that key last logged. The repetitions skipped meanwhile are counted
 * into the next line that is logged: the same line once the window is over, or a line of its own right before a
 * different line.
 *
 * @param clock milliseconds, injectable for tests.
 */
internal class LogThrottle(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {

    private class Entry(val line: String, val loggedAt: Long, val skipped: Int)

    private val entries = ConcurrentHashMap<String, Entry>()

    /**
     * Calls [log] with [line] (with the count of skipped repetitions appended) unless it repeats the previous line
     * of [key] within the window.
     */
    fun log(key: String, line: String, log: (String) -> Unit) {
        val now = clock()
        val linesToLog = mutableListOf<String>()
        entries.compute(key) { _, previous ->
            when {
                previous == null -> {
                    linesToLog += line
                    Entry(line = line, loggedAt = now, skipped = 0)
                }

                previous.line != line -> {
                    if (previous.skipped > 0) linesToLog += previous.line.repeated(previous.skipped)
                    linesToLog += line
                    Entry(line = line, loggedAt = now, skipped = 0)
                }

                now - previous.loggedAt >= windowMs -> {
                    linesToLog += if (previous.skipped > 0) line.repeated(previous.skipped) else line
                    Entry(line = line, loggedAt = now, skipped = 0)
                }

                else -> Entry(line = previous.line, loggedAt = previous.loggedAt, skipped = previous.skipped + 1)
            }
        }
        linesToLog.forEach(log)
    }

    private fun String.repeated(times: Int) = "$this (repeated $times more times)"

    /**
     * Forgets [key], e.g. once its link is deleted.
     */
    fun forget(key: String) {
        entries.remove(key)
    }

    companion object {
        private const val DEFAULT_WINDOW_MS = 60_000L
    }
}
