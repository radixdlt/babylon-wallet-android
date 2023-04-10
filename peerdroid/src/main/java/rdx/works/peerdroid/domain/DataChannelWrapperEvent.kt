package rdx.works.peerdroid.domain

// The domain model that is used by the app
sealed interface DataChannelWrapperEvent {

    // this data class holds the remote client id (from which dapp comes the message)
    // and its assembled and decoded message
    data class MessageFromRemoteClient(
        val remoteClientId: String,
        val messageInJsonString: String
    ) : DataChannelWrapperEvent

    // the data channel state between wallet and remote client (dapp) changed
    data class StateChangedForRemoteClient(
        val remoteClientId: String,
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
        val remoteClientId: String
    ) : DataChannelWrapperEvent
}
