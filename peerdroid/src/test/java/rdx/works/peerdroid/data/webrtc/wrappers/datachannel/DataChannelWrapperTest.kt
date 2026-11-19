package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.webrtc.DataChannel
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.MessageNotSentException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DataChannelWrapperTest {

    @Test
    fun `the packages of two messages sent at once never interleave`() = runBlocking {
        val sentMessageIds = Collections.synchronizedList(mutableListOf<String>())
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } returns DataChannel.State.OPEN
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

    @Test
    fun `a message sent while the data channel is still opening waits for it`() = runBlocking {
        val sent = Collections.synchronizedList(mutableListOf<DataChannel.State>())
        val state = AtomicReference(DataChannel.State.CONNECTING)
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } answers { state.get() }
            every { send(any()) } answers {
                sent.add(state.get())
                state.get() == DataChannel.State.OPEN
            }
        }
        val wrapper = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel)

        val result = async(Dispatchers.Default) { wrapper.sendMessage("""{"discriminator":"accountList"}""") }
        Thread.sleep(100)
        state.set(DataChannel.State.OPEN)

        assertTrue(result.await().isSuccess)
        assertTrue(sent.isNotEmpty() && sent.all { it == DataChannel.State.OPEN }, "sent only once open: $sent")
    }

    @Test
    fun `a message whose first package the channel does not take is not sent`() = runTest {
        var sends = 0
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } returns DataChannel.State.OPEN
            every { send(any()) } answers {
                sends++
                false
            }
        }

        val result = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel).sendMessage(LARGE_MESSAGE)

        assertIs<MessageNotSentException>(result.exceptionOrNull())
        assertEquals(1, sends, "nothing else is sent once the first package is refused")
    }

    @Test
    fun `a message whose later package the channel does not take may have been delivered, and is not sent again`() = runTest {
        var sends = 0
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } returns DataChannel.State.OPEN
            every { send(any()) } answers {
                sends++
                sends == 1
            }
        }

        val result = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel).sendMessage(LARGE_MESSAGE)

        val failure = result.exceptionOrNull()
        assertIs<IllegalStateException>(failure)
        assertFalse(failure is MessageNotSentException)
        assertEquals(2, sends, "the message is neither completed nor sent again")
    }

    @Test
    fun `a channel that stays opening holds a send for 5 s at most, then the message is not sent`() = runTest {
        var sends = 0
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } returns DataChannel.State.CONNECTING
            every { send(any()) } answers {
                sends++
                false
            }
        }

        val result = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel).sendMessage("""{"discriminator":"accountList"}""")

        assertIs<MessageNotSentException>(result.exceptionOrNull())
        assertTrue(currentTime in 5_000L..5_100L, "waited $currentTime ms")
        assertEquals(1, sends)
    }

    @Test
    fun `a message sent over a disposed channel is not sent`() = runTest {
        val dataChannel = mockk<DataChannel>(relaxed = true) {
            every { state() } throws IllegalStateException("MediaStreamTrack has been disposed.")
            every { send(any()) } throws IllegalStateException("MediaStreamTrack has been disposed.")
        }

        val result = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel).sendMessage("""{"discriminator":"accountList"}""")

        assertIs<MessageNotSentException>(result.exceptionOrNull())
    }

    private companion object {
        // Several chunks: a metadata package and more than one chunk.
        val LARGE_MESSAGE = """{"answer":"${"w".repeat(50_000)}"}"""
    }
}
