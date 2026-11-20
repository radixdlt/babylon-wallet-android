package rdx.works.peerdroid.data

import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.shortId
import timber.log.Timber

/**
 * The conventions of the Radix Connect logs.
 *
 * - Never logged: passwords, keys, message contents, persona or account data, IP addresses. A link is identified by
 *   `link=<short id>` ([ConnectionIdHolder.shortId], a prefix of the hash of its password), a message by its
 *   interaction id and its size.
 * - One line per event, `<marker> link=<short id> <what happened>[: key=value, ...]`, the transport named `webrtc`.
 *   Durations are in ms, and a timeout is always logged with its limit.
 * - Levels: [e] failed for good (the user is affected), [w] went wrong but is retried or recovers, [i] lifecycle
 *   (a transport started, connected, carried a message), [d] details.
 * - Lines that can repeat (state polling, retries) go through a [LogThrottle].
 */
internal object RadixConnectLog {

    /**
     * Connections and messages, whatever the transport.
     */
    const val TAG = "RadixConnect"

    fun e(line: String, tag: String = TAG) = Timber.tag(tag).e(line)

    fun w(line: String, tag: String = TAG) = Timber.tag(tag).w(line)

    fun i(line: String, tag: String = TAG) = Timber.tag(tag).i(line)

    fun d(line: String, tag: String = TAG) = Timber.tag(tag).d(line)
}

/**
 * `link=<short id>`: how a link appears in the logs.
 */
internal val ConnectionIdHolder.logId: String
    get() = "link=$shortId"

/**
 * The type and message of an error, for a log line, with any IP address removed.
 */
internal fun Throwable.describe(): String = "${javaClass.simpleName} ${message.orEmpty()}".trim().redactIpAddresses()

/**
 * Replaces the IP addresses (v4 and v6, with their ports) in this text, e.g. in a WebRTC or network error, with `<ip>`.
 */
internal fun String.redactIpAddresses(): String = replace(IPV4_ADDRESS, IP_PLACEHOLDER).replace(IPV6_ADDRESS, IP_PLACEHOLDER)

private const val IP_PLACEHOLDER = "<ip>"

private val IPV4_ADDRESS = Regex("""(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?(?![\w.])""")

// The full form (8 groups) or any form with "::"; times like 12:30:45 and paths like module::name are left alone.
private val IPV6_ADDRESS = Regex(
    """(?<![\w:])(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4}){7}|(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?::""" +
        """(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)(?:%\w+)?(?![\w:])"""
)
