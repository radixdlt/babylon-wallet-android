package rdx.works.peerdroid.data.webrtc.wrappers.peerconnection

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

@Suppress("EmptyFunctionBlock")
/**
 * Wrapper class of the PeerConnection.Observer that helps to
 * avoid the implementation of all the members in the actual callback.
 * See the: createPeerConnectionFlow
 *
 */
internal open class PeerConnectionObserver : PeerConnection.Observer {

    override fun onIceCandidate(p0: IceCandidate?) {
    }

    override fun onDataChannel(p0: DataChannel?) {
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
    }

    override fun onAddStream(p0: MediaStream?) {
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
    }

    override fun onRemoveStream(p0: MediaStream?) {
    }

    override fun onRenegotiationNeeded() {
    }
}
