package rdx.works.peerdroid.data.webrtc.model

// wrapper class for the PeerConnection.Observer events
internal sealed interface PeerConnectionEvent {

    data class SignalingState(
        val message: String
    ) : PeerConnectionEvent

    data class IceGatheringChange(
        val state: State
    ) : PeerConnectionEvent {

        enum class State {
            NEW, GATHERING, COMPLETE, UNKNOWN
        }
    }

    data class IceCandidate(
        val data: Data
    ) : PeerConnectionEvent {

        data class Data(
            val candidate: String, // this is the sdp value
            val sdpMid: String,
            val sdpMLineIndex: Int
        )
    }

    object RenegotiationNeeded : PeerConnectionEvent

    object Connected : PeerConnectionEvent

    object Disconnected : PeerConnectionEvent
}
