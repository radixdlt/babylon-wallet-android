package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.webrtc.DataChannel
import rdx.works.peerdroid.domain.ConnectionIdHolder
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataChannelWrapperTest {

    @Test
    fun `the packages of two messages sent at once never interleave`() = runBlocking {
        val sentMessageIds = Collections.synchronizedList(mutableListOf<String>())
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { send(any()) } answers {
                val buffer = firstArg<DataChannel.Buffer>().data
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                val packageJson = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                sentMessageIds.add(packageJson.getValue("messageId").jsonPrimitive.content)
                // A slow channel: the other sender has every chance to get in between.
                Thread.sleep(2)
                true
            }
        }
        val wrapper = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel)
        // Several chunks each.
        val messages = listOf("a", "b").map { letter -> """{"payload":"${letter.repeat(50_000)}"}""" }

        val results = messages.map { message ->
            async(Dispatchers.Default) { wrapper.sendMessage(message) }
        }.awaitAll()

        assertTrue(results.all { it.isSuccess })
        val runs = sentMessageIds.zipWithNext().count { (previous, next) -> previous != next }
        assertEquals(1, runs, "each message's packages are sent together: $sentMessageIds")
    }
}
