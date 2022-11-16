package rdx.works.peerdroid.data.webrtc.model

import org.webrtc.IceCandidate

// Kotlin wrapper class for the WebRTC IceCandidate class
// IceCandidate:
// https://chromium.googlesource.com/external/webrtc/+/master/sdk/android/api/org/webrtc/IceCandidate.java
internal data class RemoteIceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
) {

    companion object {

        fun RemoteIceCandidate.toWebRtcIceCandidate() = IceCandidate(
            sdpMid,
            sdpMLineIndex,
            candidate
        )
    }
}
