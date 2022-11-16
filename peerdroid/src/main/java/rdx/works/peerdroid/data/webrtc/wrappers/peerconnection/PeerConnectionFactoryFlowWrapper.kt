package rdx.works.peerdroid.data.webrtc.wrappers.peerconnection

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent

/**
 * Transform the PeerConnection.Observer callback to flow.
 *
 * It returns a PeerConnectionEvent.
 *
 */
internal fun PeerConnectionFactory.createPeerConnectionFlow(
    rtcConfiguration: PeerConnection.RTCConfiguration,
    initializePeerConnection: (PeerConnection?) -> Unit,
    createRtcDataChannel: () -> Unit
) = callbackFlow {

    val observer = object : PeerConnectionObserver() {
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
            super.onSignalingChange(p0)
            trySend(
                element = PeerConnectionEvent.SignalingState(message = p0?.name ?: "no signaling state")
            )
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
            super.onIceGatheringChange(p0)

            var state = PeerConnectionEvent.IceGatheringChange.State.UNKNOWN
            when (p0) {
                PeerConnection.IceGatheringState.GATHERING -> {
                    state = PeerConnectionEvent.IceGatheringChange.State.GATHERING
                }
                PeerConnection.IceGatheringState.COMPLETE -> {
                    state = PeerConnectionEvent.IceGatheringChange.State.COMPLETE
                }
                PeerConnection.IceGatheringState.NEW -> {
                    state = PeerConnectionEvent.IceGatheringChange.State.NEW
                }
                null -> PeerConnectionEvent.IceGatheringChange.State.UNKNOWN
            }

            trySend(element = PeerConnectionEvent.IceGatheringChange(state = state))
        }

        override fun onIceCandidate(p0: IceCandidate?) {
            super.onIceCandidate(p0)
            p0?.let { iceCandidate ->
                trySend(
                    element = PeerConnectionEvent.IceCandidate(
                        data = PeerConnectionEvent.IceCandidate.Data(
                            candidate = iceCandidate.sdp,
                            sdpMid = iceCandidate.sdpMid,
                            sdpMLineIndex = iceCandidate.sdpMLineIndex
                        )
                    )
                )
            }
        }

        override fun onRenegotiationNeeded() {
            super.onRenegotiationNeeded()
            trySend(PeerConnectionEvent.RenegotiationNeeded)
        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
            super.onIceConnectionChange(p0)
            if (p0 == PeerConnection.IceConnectionState.FAILED || p0 == PeerConnection.IceConnectionState.DISCONNECTED) {
                trySend(PeerConnectionEvent.Disconnected)
            }
        }
    }

    val peerConnection = createPeerConnection(rtcConfiguration, observer)
    initializePeerConnection(peerConnection)
    // right after the initialization of the peer connection, create a RTC data channel
    createRtcDataChannel()

    /*
     * awaitClose should be used to keep the flow running,
     * otherwise the channel will be closed immediately when block completes.
     *
     * awaitClose argument is called either when a flow consumer cancels the flow collection or
     * when a callback-based API invokes SendChannel.close manually and
     * is typically used to cleanup the resources after the completion, e.g. unregister a callback.
     */
    awaitClose {
        Log.d("WEB_RTC", "peer connection: awaitClose")
        // peerConnection?.dispose() // TODO is dispose or close needed?
    }
}
