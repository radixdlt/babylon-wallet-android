package rdx.works.peerdroid.data.webrtc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import rdx.works.peerdroid.domain.ConnectionIdHolder
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.RemoteClientHolder
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DataChannelReadersTest {

    private val link = ConnectionIdHolder(id = "link")

    private fun message(text: String) = DataChannelWrapperEvent.MessageFromRemoteConnectionId(
        connectionIdHolder = link,
        messageInJsonString = text
    )

    /** A data channel as its reader sees it: what it delivers, and whether it is being read. */
    private class FakeChannel {
        private val arriving = MutableSharedFlow<DataChannelWrapperEvent?>(extraBufferCapacity = 64)
        var readers = 0
            private set

        /** Like a channel's event flow: one observer while collected, ending when the channel closes (`null`). */
        val events: Flow<DataChannelWrapperEvent> = flow {
            arriving.takeWhile { it != null }.collect { event -> emit(requireNotNull(event)) }
        }.onStart { readers++ }.onCompletion { readers-- }

        fun deliver(event: DataChannelWrapperEvent) {
            arriving.tryEmit(event)
        }

        fun close() {
            arriving.tryEmit(null)
        }
    }

    private fun TestScope.listen(readers: DataChannelReaders): MutableList<DataChannelWrapperEvent> {
        val heard = mutableListOf<DataChannelWrapperEvent>()
        backgroundScope.launch { readers.messages.collect { heard += it } }
        runCurrent()
        return heard
    }

    /**
     * The defect seen on a phone: 16 channels opened and closed since the app started, and the 17th was never read.
     * Readers end with their channel, so the 21st is read like the first.
     */
    @Test
    fun any_number_of_channels_can_come_and_go() = runTest(UnconfinedTestDispatcher()) {
        val readers = DataChannelReaders(backgroundScope)
        val heard = listen(readers)
        repeat(20) { n ->
            val channel = FakeChannel()
            readers.start(RemoteClientHolder(id = "client-$n"), channel.events)
            runCurrent()
            channel.close()
            runCurrent()
        }
        assertEquals(0, readers.count, "every closed channel's reader ended")

        val latest = FakeChannel()
        readers.start(RemoteClientHolder(id = "client-21"), latest.events)
        runCurrent()
        latest.deliver(message("the request"))
        runCurrent()
        assertEquals<List<DataChannelWrapperEvent>>(listOf(message("the request")), heard)
    }

    /** A link's old connection ending does not stop the reader of its new one. */
    @Test
    fun an_old_connection_ending_leaves_the_new_one_read() = runTest(UnconfinedTestDispatcher()) {
        val readers = DataChannelReaders(backgroundScope)
        val heard = listen(readers)
        val old = FakeChannel()
        val new = FakeChannel()
        readers.start(RemoteClientHolder(id = "old"), old.events)
        readers.start(RemoteClientHolder(id = "new"), new.events)
        runCurrent()

        readers.stop(RemoteClientHolder(id = "old"))
        runCurrent()
        new.deliver(message("on the new connection"))
        runCurrent()

        assertEquals(0, old.readers, "the old channel is let go of")
        assertEquals(1, new.readers)
        assertEquals<List<DataChannelWrapperEvent>>(listOf(message("on the new connection")), heard)
    }

    /** Two channels opening at once are both read: none is lost because the other came a moment later. */
    @Test
    fun channels_opening_together_are_all_read() = runTest(UnconfinedTestDispatcher()) {
        val readers = DataChannelReaders(backgroundScope)
        val heard = listen(readers)
        val first = FakeChannel()
        val second = FakeChannel()
        readers.start(RemoteClientHolder(id = "first"), first.events)
        readers.start(RemoteClientHolder(id = "second"), second.events)
        runCurrent()

        first.deliver(message("one"))
        second.deliver(message("two"))
        runCurrent()

        assertEquals<Set<DataChannelWrapperEvent>>(setOf(message("one"), message("two")), heard.toSet())
    }

    /** Nobody listening: the channel is not read (nothing is taken, so nothing is confirmed), until somebody does. */
    @Test
    fun a_channel_is_read_only_while_somebody_listens() = runTest(UnconfinedTestDispatcher()) {
        val readers = DataChannelReaders(backgroundScope)
        val channel = FakeChannel()
        readers.start(RemoteClientHolder(id = "client"), channel.events)
        runCurrent()
        assertEquals(0, channel.readers, "unheard, the channel is not read")

        val listener = backgroundScope.launch { readers.messages.collect {} }
        runCurrent()
        assertEquals(1, channel.readers)

        listener.cancel()
        runCurrent()
        assertEquals(0, channel.readers, "read again only once somebody listens again")
        assertEquals(1, readers.count, "the reader waits for a listener; it ends with the channel or when stopped")
    }

    /** Stopping every reader lets go of every channel. */
    @Test
    fun stopping_all_lets_go_of_every_channel() = runTest(UnconfinedTestDispatcher()) {
        val readers = DataChannelReaders(backgroundScope)
        listen(readers)
        val channels = List(3) { FakeChannel() }
        channels.forEachIndexed { n, channel -> readers.start(RemoteClientHolder(id = "client-$n"), channel.events) }
        runCurrent()

        readers.stopAll()
        runCurrent()

        assertEquals(listOf(0, 0, 0), channels.map { it.readers })
        assertEquals(0, readers.count)
    }
}
