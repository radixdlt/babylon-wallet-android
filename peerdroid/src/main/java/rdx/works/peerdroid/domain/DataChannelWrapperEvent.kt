package rdx.works.peerdroid.domain

// The domain model that is used by the app
sealed interface DataChannelWrapperEvent {

    // this data class holds the remote connector id (from which CE comes the message)
    // and its assembled and decoded message
    data class MessageFromRemoteConnectionId(
        val connectionIdHolder: ConnectionIdHolder,
        val messageInJsonString: String
    ) : DataChannelWrapperEvent

    // the data channel state between wallet and remote connector (CE) changed
    data class StateChangedForRemoteConnector(
        val connectionIdHolder: ConnectionIdHolder,
        val state: State
    ) : DataChannelWrapperEvent {

        enum class State {
            CONNECTING,
            OPEN,
            CLOSING,
            CLOSED,
        }
    }

    data class Error(
        val connectionIdHolder: ConnectionIdHolder
    ) : DataChannelWrapperEvent
}
