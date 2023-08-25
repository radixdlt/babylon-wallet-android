package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import org.webrtc.DataChannel
import rdx.works.peerdroid.data.PackageDto
import rdx.works.peerdroid.domain.DataChannelWrapperEvent.StateChangedForRemoteConnector

// Data model that is used for the DataChannel.eventFlow wrapper:
// 1. it encapsulates the native DataChannel.State
// 2. it decodes and parses the incoming message to the corresponding type of PackageDto:
//    metadata, chuck, ReceiveMessageConfirmation, ReceiveMessageError
internal sealed interface DataChannelMessage {

    data class StateChanged(val state: DataChannel.State) : DataChannelMessage

    sealed interface Message : DataChannelMessage {

        data class MetaData(val metadataDto: PackageDto.MetaData) : Message

        data class Chunk(val chunkDto: PackageDto.Chunk) : Message
    }

    // a notification from the other peer (CE) to confirm that:
    sealed interface RemoteConnectorReceivedMessage : DataChannelMessage {
        // complete message received and assembled successfully
        data class Confirmation(val messageId: String) : RemoteConnectorReceivedMessage

        // failed to assemble complete message
        data class Error(val messageId: String) : RemoteConnectorReceivedMessage
    }

    // when something unexpected happens ...
    data class UnknownError(
        val message: String? = null
    ) : DataChannelMessage
}

// Data model that is used for the DataChannelWrapper only internally!
// The events are processed and then (some of them) are mapped to the domain model DataChannelWrapperEvent.
internal sealed interface DataChannelEvent {
    // it holds the list with all the chunks of the incoming message
    data class CompleteMessage(
        val messageId: String, // mostly for debugging reasons
        val listOfChunks: List<PackageDto.Chunk>
    ) : DataChannelEvent

    sealed interface ReceiveMessage : DataChannelEvent {

        data class Confirmation(val messageId: String) : ReceiveMessage

        data class Error(val messageId: String) : ReceiveMessage
    }

    data class StateChanged(
        val state: StateChangedForRemoteConnector.State
    ) : DataChannelEvent

    object Error : DataChannelEvent
}

internal fun DataChannel.State.toDomainModel(): DataChannelEvent.StateChanged {
    return when (this) {
        DataChannel.State.CONNECTING -> {
            DataChannelEvent.StateChanged(
                state = StateChangedForRemoteConnector.State.CONNECTING
            )
        }
        DataChannel.State.OPEN -> {
            DataChannelEvent.StateChanged(
                state = StateChangedForRemoteConnector.State.OPEN
            )
        }
        DataChannel.State.CLOSING -> {
            DataChannelEvent.StateChanged(
                state = StateChangedForRemoteConnector.State.CLOSING
            )
        }
        DataChannel.State.CLOSED -> {
            DataChannelEvent.StateChanged(
                state = StateChangedForRemoteConnector.State.CLOSED
            )
        }
    }
}
