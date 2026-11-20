package rdx.works.peerdroid.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException
import rdx.works.peerdroid.domain.shortId
import timber.log.Timber

/**
 * The conventions of the Radix Connect logs, shared by both transports and by the composite connector and link.
 *
 * - Never logged: passwords, keys, message contents, persona or account data, IP addresses. A link is identified by
 *   `link=<short id>` ([ConnectionIdHolder.shortId], a prefix of the hash of its password), a message by its
 *   interaction id and its size.
 * - One line per event, `<marker> link=<short id> <what happened>[: key=value, ...]`, transports named `iroh` and
 *   `webrtc`. Durations are in ms, and a timeout is always logged with its limit.
 * - Levels: [e] failed for good (the user is affected), [w] went wrong but is retried or recovers, [i] lifecycle
 *   (a transport started, connected, won the linking, carried a message), [d] details.
 * - Lines that can repeat (state polling, retries) go through a [LogThrottle].
 */
internal object RadixConnectLog {

    /**
     * Connections and messages, whatever the transport.
     */
    const val TAG = "RadixConnect"

    /**
     * Linking a new connector extension.
     */
    const val LINKING_TAG = "LinkingCE"

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
 * How a transport appears in the logs and diagnostics.
 */
internal val PeerdroidTransport.logName: String
    get() = name.lowercase()

/**
 * `interaction=<id> (<size> chars)`: how a message appears in the logs, never with its contents.
 */
internal fun describeMessage(message: String): String = "interaction=${interactionIdOf(message) ?: "?"} (${message.length} chars)"

/**
 * The top-level `interactionId` of a wallet interaction or a ledger message, or null when there is none. Only an id
 * that looks like one is returned, so that nothing else from the message can end up in a log.
 */
internal fun interactionIdOf(message: String): String? = runCatching {
    val interactionId = (Json.parseToJsonElement(message) as? JsonObject)?.get(INTERACTION_ID_KEY) as? JsonPrimitive
    interactionId?.takeIf { it.isString }?.content
}.getOrNull()?.takeIf { id ->
    id.length in 1..MAX_INTERACTION_ID_LENGTH && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}

/**
 * Why a message was not sent, in a few words: whether it may have reached the connector extension (only a message
 * that was "not sent" may go over another transport), and the type of the error.
 */
internal fun Throwable.sendFailureCategory(): String {
    val delivery = if (this is MessageNotSentException) "not sent" else "maybe delivered"
    return "$delivery, ${javaClass.simpleName}"
}

/**
 * The type and message of an error, for a log line, with any IP address removed.
 */
internal fun Throwable.describe(): String = "${javaClass.simpleName} ${message.orEmpty()}".trim().redactIpAddresses()

/**
 * Replaces the IP addresses (v4 and v6, with their ports) in this text, e.g. in an error of the iroh library, with
 * `<ip>`.
 */
internal fun String.redactIpAddresses(): String = replace(IPV4_ADDRESS, IP_PLACEHOLDER).replace(IPV6_ADDRESS, IP_PLACEHOLDER)

private const val INTERACTION_ID_KEY = "interactionId"
private const val MAX_INTERACTION_ID_LENGTH = 64
private const val IP_PLACEHOLDER = "<ip>"

private val IPV4_ADDRESS = Regex("""(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?(?![\w.])""")

// The full form (8 groups) or any form with "::"; times like 12:30:45 and Rust paths like iroh::net are left alone.
private val IPV6_ADDRESS = Regex(
    """(?<![\w:])(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4}){7}|(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?::""" +
        """(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)(?:%\w+)?(?![\w:])"""
)
