package rdx.works.peerdroid.data.webrtc.wrappers.datachannel

import rdx.works.peerdroid.domain.BasePackage

// wrapper class for the WebRTC DataChannel.Observer events
// events can be either data channel related (e.g. state changed)
// or incoming messages from the other peer
sealed interface DataChannelEvent {

    // This express the incoming messages from the other peer
    sealed interface IncomingMessage : DataChannelEvent {

        // an incoming package that can be metadata or chunk
        // this will later transformed to Message (see below)
        data class Package(
            val messageInListOfPackages: List<BasePackage>
        ) : IncomingMessage

        // this data class holds the assembled and decoded message
        data class Message(
            val decodedMessage: String
        ) : IncomingMessage

        // a confirmation notification from the other peer to confirm that
        // it received and correctly assembled the message
        object ConfirmationNotification : IncomingMessage

        // an error notification from the other peer to warn that
        // it received but failed to assemble the message
        object ErrorNotification : IncomingMessage

        object MessageHashMismatch : IncomingMessage
    }

    enum class StateChanged : DataChannelEvent {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSE,
        UNKNOWN
    }

    object BufferedAmountChange : DataChannelEvent

    // when something unexpected happens ...
    data class UnknownError(
        val message: String? = null
    ) : DataChannelEvent
}
