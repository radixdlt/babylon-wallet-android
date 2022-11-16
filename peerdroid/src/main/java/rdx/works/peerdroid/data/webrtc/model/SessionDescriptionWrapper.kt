package rdx.works.peerdroid.data.webrtc.model

import org.webrtc.SessionDescription

// Kotlin wrapper class for the WebRTC SessionDescription class
// SessionDescription:
// https://chromium.googlesource.com/external/webrtc/+/HEAD/sdk/android/api/org/webrtc/SessionDescription.java
internal data class SessionDescriptionWrapper(
    val type: Type,
    val sessionDescriptionValue: SessionDescriptionValue
) {

    enum class Type {
        OFFER, ANSWER
    }

    // contains the description of the WebRTC session description
    @JvmInline
    value class SessionDescriptionValue(val sdp: String)

    companion object {

        fun SessionDescriptionWrapper.toWebRtcSessionDescription() = SessionDescription(
            when (type) {
                Type.ANSWER -> {
                    SessionDescription.Type.ANSWER
                }
                Type.OFFER -> {
                    SessionDescription.Type.OFFER
                }
            },
            sessionDescriptionValue.sdp
        )
    }
}
