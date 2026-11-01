package rdx.works.peerdroid.data.webrtc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import rdx.works.peerdroid.domain.DataChannelWrapperEvent
import rdx.works.peerdroid.domain.RemoteClientHolder
import java.util.concurrent.ConcurrentHashMap

/**
 * The readers of the open data channels: one per channel, living exactly as long as the channel, all handing their
 * events to [messages].
 *
 * - A channel is read only while somebody collects [messages]. Meanwhile the wallet takes nothing from it, so it
 *   confirms nothing and the connector extension sends the message again later.
 * - A channel's reader ends when its events end (the channel closed) or when it is [stop]ped, whichever comes first,
 *   so any number of channels can come and go over the app's life.
 * - Each reader belongs to one remote client: a link's new connection starting, or its old one ending, never touches
 *   the other's reader.
 *
 * This replaces a `flatMapMerge` over the latest channel of a `MutableStateFlow`, whose readers never ended: it reads
 * at most 16 flows at once, so after 16 channels since the app started a new one was never read, and every request on
 * it was lost until the app restarted. A state flow also keeps only the latest value, so of two channels opening at
 * once one could go unread.
 */
internal class DataChannelReaders(private val scope: CoroutineScope) {

    private val incoming = MutableSharedFlow<DataChannelWrapperEvent>()

    val messages: SharedFlow<DataChannelWrapperEvent> = incoming.asSharedFlow()

    private val readers = ConcurrentHashMap<RemoteClientHolder, Job>()

    /** How many channels have a reader right now. */
    val count: Int
        get() = readers.size

    /** Reads [events] — the channel of [remoteClient] — until they end or [stop] is called for it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(remoteClient: RemoteClientHolder, events: Flow<DataChannelWrapperEvent>) {
        val reader = scope.launch(start = CoroutineStart.LAZY) {
            incoming.subscriptionCount
                .map { subscribers -> subscribers > 0 }
                .distinctUntilChanged()
                // Reading while somebody listens, stopped (and the channel let go of) while nobody does.
                .transformLatest { listened ->
                    if (listened) {
                        events.collect { event ->
                            incoming.subscriptionCount.first { subscribers -> subscribers > 0 }
                            incoming.emit(event)
                        }
                        emit(Unit) // the channel's events ended: so does its reader
                    }
                }
                .first()
        }
        readers.put(remoteClient, reader)?.cancel()
        reader.invokeOnCompletion { readers.remove(remoteClient, reader) }
        reader.start()
    }

    /** Stops reading the channel of [remoteClient]: its events flow is cancelled, which lets go of the channel. */
    fun stop(remoteClient: RemoteClientHolder) {
        readers.remove(remoteClient)?.cancel()
    }

    /** Stops reading every channel. */
    fun stopAll() {
        readers.keys.toList().forEach(::stop)
    }
}
