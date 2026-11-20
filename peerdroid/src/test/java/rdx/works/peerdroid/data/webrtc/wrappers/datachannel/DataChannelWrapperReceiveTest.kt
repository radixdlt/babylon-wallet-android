package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.messagechunking.splitMessage
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * While the wallet is sending a message, the receipt confirmation of an incoming message waits for the send to finish.
 * The messages that arrive in the meantime must not be lost.
 */
class DataChannelWrapperReceiveTest {

    private val observer = slot<DataChannel.Observer>()
    private val sendStarted = CountDownLatch(1)
    private val releaseSend = CountDownLatch(1)

    private val dataChannel = mockk<DataChannel>(relaxed = true) {
        every { state() } returns DataChannel.State.OPEN
        every { registerObserver(capture(observer)) } answers { }
        every { label() } returns "link"
        every { send(any()) } answers {
            val buffer = firstArg<DataChannel.Buffer>().data
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val packageType = Json.parseToJsonElement(bytes.decodeToString()).jsonObject.getValue("packageType").jsonPrimitive.content
            if (packageType == "metaData" || packageType == "chunk") {
                // The wallet's own message: a slow channel, held until the test lets it go.
                sendStarted.countDown()
                releaseSend.await(WAIT_LIMIT_S, TimeUnit.SECONDS)
            }
            true
        }
    }

    private val wrapper = DataChannelWrapper(ConnectionIdHolder("link"), dataChannel)

    private fun receive(packageDto: PackageDto) {
        val json = Json.encodeToString(PackageDto.serializer(), packageDto)
        observer.captured.onMessage(DataChannel.Buffer(ByteBuffer.wrap(json.encodeToByteArray()), false))
    }

    private fun receiveMessage(message: String) = splitMessage(message.toByteArray()).forEach(::receive)

    private fun runScenario(incomingSecond: String, thenReceive: List<PackageDto>): List<String> = runBlocking {
        val received = Collections.synchronizedList(mutableListOf<String>())
        val collector = launch(Dispatchers.Default) {
            wrapper.dataChannelEvents.collect { event ->
                if (event is DataChannelWrapperEvent.MessageFromRemoteConnectionId) received.add(event.messageInJsonString)
            }
        }
        withTimeout(WAIT_LIMIT_S * 1_000) { while (!observer.isCaptured) delay(10) }
        // The first message split in a test JVM loads the native library of its hash function, which took up to 20 s:
        // done here, before the scenario and its waits.
        splitMessage("warm-up".toByteArray())

        // The wallet starts sending a message and the channel is slow.
        val outgoing = async(Dispatchers.Default) { wrapper.sendMessage("""{"answer":"${"w".repeat(50_000)}"}""") }
        assertTrue(sendStarted.await(WAIT_LIMIT_S, TimeUnit.SECONDS))

        // Meanwhile the dApp sends a first message: its confirmation waits for the wallet's send.
        receiveMessage("""{"request":"first"}""")
        delay(300)
        // and a second message, followed by other packages.
        receiveMessage(incomingSecond)
        thenReceive.forEach(::receive)
        delay(300)

        releaseSend.countDown()
        assertTrue(outgoing.await().isSuccess)
        withTimeoutOrNull(WAIT_LIMIT_S * 1_000) { while (received.size < 2) delay(20) }
        collector.cancel()
        received.toList()
    }

    @Test
    fun `a message that arrives while the previous one waits for its confirmation is not lost`() {
        val second = """{"request":"second"}"""
        // The connector's confirmation of the wallet's message arrives right after the second message.
        val received = runScenario(second, thenReceive = listOf(PackageDto.ReceiveMessageConfirmation(messageId = "any")))

        assertEquals(listOf("""{"request":"first"}""", second), received)
    }

    @Test
    fun `a large message that arrives while the previous one waits for its confirmation is not lost`() {
        // More chunks than the default buffer of the data channel event flow (64).
        val second = """{"request":"${"d".repeat(100 * 15_441)}"}"""
        val received = runScenario(second, thenReceive = emptyList())

        assertEquals(2, received.size, "messages received: ${received.map { it.take(30) }}")
        assertEquals(second, received[1])
    }

    private companion object {
        // Only a bound for a broken run, never reached otherwise: the waits end as soon as their event happens.
        const val WAIT_LIMIT_S = 30L
    }
}
