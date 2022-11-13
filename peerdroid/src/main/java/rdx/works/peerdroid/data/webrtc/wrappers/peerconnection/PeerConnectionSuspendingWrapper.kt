package rdx.works.peerdroid.data.webrtc.wrappers.peerconnection

import android.util.Log
import org.webrtc.AddIceObserver
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate.Companion.toWebRtcIceCandidate
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.Companion.toWebRtcSessionDescription
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.helpers.Result
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Transform the PeerConnection.createOffer callback to a suspend function.
 *
 * It returns a [Result].
 *
 */
internal suspend fun PeerConnection.createSuspendingOffer(
    mediaConstraints: MediaConstraints
): Result<SessionDescriptionValue> = suspendCoroutine { continuation ->

    val observer = object : SdpObserver {
        // from this suspending callback we are only interested in creating an offer
        // thus return success only in case of the "onCreateSuccess"
        override fun onCreateSuccess(p0: SessionDescription?) {
            Log.d("WEB_RTC", "WebRTC peer connection created successfully an offer")

            p0?.let {
                continuation.resume(
                    Result.Success(
                        SessionDescriptionValue(p0.description)
                    )
                )
            } ?: continuation.resume(
                Result.Error("sessionDescription is null")
            )
        }

        override fun onSetSuccess() {
            Log.d("WEB_RTC", "createOffer, onSetSuccess")
            continuation.resume(Result.Error(""))
        }

        override fun onCreateFailure(p0: String?) {
            Log.d("WEB_RTC", "createOffer, onCreateFailure $p0")
            continuation.resume(Result.Error("failed to create offer: $p0"))
        }

        override fun onSetFailure(p0: String?) {
            Log.d("WEB_RTC", "createOffer, onSetFailure: $p0")
            continuation.resume(Result.Error(""))
        }
    }
    createOffer(observer, mediaConstraints)
}

/**
 * Transform the PeerConnection.setLocalDescription callback to a suspend function.
 *
 * It returns a [Result].
 *
 */
internal suspend fun PeerConnection.setSuspendingLocalDescription(
    sessionDescription: SessionDescriptionWrapper
): Result<Unit> = suspendCoroutine { continuation ->

    val observer = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {
            Log.d("WEB_RTC", "created successfully local session description: $p0")
            // we want to SET the local session description, not to create one
            // thus return Error result
            continuation.resume(Result.Error("on create success"))
        }

        override fun onSetSuccess() {
            Log.d("WEB_RTC", "set successfully local session description")
            continuation.resume(Result.Success(Unit))
        }

        override fun onCreateFailure(p0: String?) {
            Log.d("WEB_RTC", "failed to create local session description: $p0")
            continuation.resume(Result.Error("on create failure"))
        }

        override fun onSetFailure(p0: String?) {
            Log.d("WEB_RTC", "failed to set local session description: $p0")
            continuation.resume(Result.Error("on set failure"))
        }
    }

    setLocalDescription(observer, sessionDescription.toWebRtcSessionDescription())
}

/**
 * Transform the PeerConnection.setRemoteDescription callback to a suspend function.
 *
 * It returns a [Result].
 *
 */
internal suspend fun PeerConnection.setSuspendingRemoteDescription(
    sessionDescription: SessionDescriptionWrapper
): Result<Unit> = suspendCoroutine { continuation ->

    val observer = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {
            Log.d("WEB_RTC", "created successfully remote session description: $p0")
            // we want to SET the remote session description, not to create one
            // thus return Error result
            continuation.resume(Result.Error("on create success"))
        }

        override fun onSetSuccess() {
            Log.d("WEB_RTC", "set successfully remote session description")
            continuation.resume(Result.Success(Unit))
        }

        override fun onCreateFailure(p0: String?) {
            Log.d("WEB_RTC", "failed to create remote session description: $p0")
            continuation.resume(Result.Error("on create failure"))
        }

        override fun onSetFailure(p0: String?) {
            Log.d("WEB_RTC", "failed to set remote session description: $p0")
            continuation.resume(Result.Error("on set failure"))
        }
    }

    setRemoteDescription(observer, sessionDescription.toWebRtcSessionDescription())
}

/**
 * Transform the PeerConnection.addIceCandidate callback to a suspend function.
 *
 * It returns a [Boolean].
 *
 */
internal suspend fun PeerConnection.addSuspendingIceCandidate(
    remoteIceCandidate: RemoteIceCandidate
): Boolean = suspendCoroutine { continuation ->

    val observer = object : AddIceObserver {
        override fun onAddSuccess() {
            Log.d("WEB_RTC", "added successfully ice candidate")
            continuation.resume(true)
        }

        override fun onAddFailure(p0: String?) {
            Log.e("WEB_RTC", "failed to add ice candidate: $p0")
            continuation.resume(false)
        }
    }

    addIceCandidate(remoteIceCandidate.toWebRtcIceCandidate(), observer)
}
