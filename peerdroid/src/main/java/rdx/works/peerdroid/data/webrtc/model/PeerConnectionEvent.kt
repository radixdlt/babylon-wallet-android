package rdx.works.peerdroid.data.webrtc.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile

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

    object Failed : PeerConnectionEvent
}

/**
 * This is a helper function for adding a new connection in wallet settings.
 * In order to get the disconnected event, it means the peer connection was first connected.
 *
 */
internal fun Flow<PeerConnectionEvent>.completeWhenDisconnected(): Flow<PeerConnectionEvent> =
    transformWhile { event ->
        emit(event)
        event !is PeerConnectionEvent.Disconnected
    }
