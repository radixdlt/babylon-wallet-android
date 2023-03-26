package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import rdx.works.peerdroid.domain.BasePackage

// wrapper class for the WebRTC DataChannel.Observer events
// events can be either data channel related (e.g. state changed)
// or incoming messages from the other peer
sealed interface DataChannelEvent {

    // This express the incoming messages from the other peer
    sealed interface IncomingMessage : DataChannelEvent {

        // an incoming message that can be metadata or chunk
        // this will later transformed to DecodedMessage (see below)
        data class Package(
            val messageInListOfPackages: List<BasePackage>
        ) : IncomingMessage

        // this data class holds the remote client id (from which dapp comes the message)
        // and its assembled and decoded message
        data class DecodedMessage(
            val remoteClientId: String,
            val message: String
        ) : IncomingMessage

        // a confirmation notification from the other peer to confirm that
        // message received and assembled successfully
        data class ConfirmationNotification(
            val messageId: String
        ) : IncomingMessage

        // an error notification from the other peer to warn that
        // message received but failed to assemble it
        data class ErrorNotification(
            val messageId: String
        ) : IncomingMessage

        object MessageHashMismatch : IncomingMessage
    }

    enum class StateChanged : DataChannelEvent {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSE,
        DELETE_CONNECTION,
        UNKNOWN
    }

    // when something unexpected happens ...
    data class UnknownError(
        val message: String? = null
    ) : DataChannelEvent
}
