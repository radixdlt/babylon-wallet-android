package rdx.works.peerdroid.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import rdx.works.peerdroid.domain.ConnectionIdHolder
import java.util.concurrent.ConcurrentHashMap

/**
 * The dApp requests the app took from each link over WebRTC and has not answered: what the wallet tells its connector
 * it still has, on every channel it opens (a `walletRequests` message). Kept in memory, the list is lost with the app,
 * exactly when the requests are: a connector that knows it then sends again the ones it had delivered, and never one
 * the wallet kept (it was only in the background). Over iroh the library does the same by itself.
 *
 * Requests are read from the messages themselves (`interactionId`, `items`, an answer's `discriminator`); anything
 * else is ignored.
 */
internal class HeldRequests {

    private val byLink = ConcurrentHashMap<ConnectionIdHolder, LinkedHashSet<String>>()

    /** The app took [message] from the connector of [link]. */
    fun took(link: ConnectionIdHolder, message: String) {
        when (val tracked = classify(message)) {
            is Tracked.Request -> update(link) { held ->
                held.add(tracked.id)
                while (held.size > MAX_TRACKED) held.remove(held.first())
            }
            is Tracked.Cancel -> update(link) { held -> held.remove(tracked.id) }
            else -> Unit
        }
    }

    /** The app sent [message] to the connector of [link], and it was delivered. */
    fun sent(link: ConnectionIdHolder, message: String) {
        val tracked = classify(message)
        if (tracked is Tracked.Response) update(link) { held -> held.remove(tracked.id) }
    }

    /** The message telling the connector of [link] which of its requests the app still has. */
    fun inventoryMessage(link: ConnectionIdHolder): String {
        val ids = byLink[link]?.let { held -> synchronized(held) { held.toList() } }.orEmpty()
        return walletRequestsMessage(ids)
    }

    private fun update(link: ConnectionIdHolder, change: (LinkedHashSet<String>) -> Unit) {
        val held = byLink.getOrPut(link) { LinkedHashSet() }
        synchronized(held) { change(held) }
    }

    private sealed interface Tracked {
        val id: String

        data class Request(override val id: String) : Tracked
        data class Cancel(override val id: String) : Tracked
        data class Response(override val id: String) : Tracked
    }

    @Suppress("ReturnCount")
    private fun classify(message: String): Tracked? {
        val json = runCatching { Json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return null
        val id = json.string("interactionId") ?: return null
        // An answer has items too: its discriminator tells it apart.
        when (json.string("discriminator")) {
            "success", "failure" -> return Tracked.Response(id)
            null -> Unit
            else -> return null
        }
        val items = json["items"] as? JsonObject ?: return null
        return if (items.string("discriminator") == "cancelRequest") Tracked.Cancel(id) else Tracked.Request(id)
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private companion object {
        /** Requests kept per link, newest kept: one never answered is dropped at last. */
        const val MAX_TRACKED = 200
    }
}

/** The message telling a connector which of its requests the app still has. */
internal fun walletRequestsMessage(interactionIds: List<String>): String =
    Json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "discriminator" to JsonPrimitive("walletRequests"),
                "interactionIds" to JsonArray(interactionIds.map(::JsonPrimitive))
            )
        )
    )
