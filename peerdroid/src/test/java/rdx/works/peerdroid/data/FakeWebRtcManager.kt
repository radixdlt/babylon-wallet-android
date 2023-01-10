package rdx.works.peerdroid.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.webrtc.DataChannel
import rdx.works.peerdroid.data.webrtc.WebRtcManager
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result
import javax.inject.Inject

internal class FakeWebRtcManager @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val withError: Boolean = false
) : WebRtcManager {

    override fun createPeerConnection(connectionId: String): Flow<PeerConnectionEvent> {
        return flow {
            emit(PeerConnectionEvent.RenegotiationNeeded)
            delay(100)
            emit(
                PeerConnectionEvent.IceGatheringChange(
                    state = PeerConnectionEvent.IceGatheringChange.State.GATHERING
                )
            )
            delay(100)
            emit(
                PeerConnectionEvent.IceCandidate(
                    data = PeerConnectionEvent.IceCandidate.Data(
                        candidate = "candidate",
                        sdpMid = "sdpMid",
                        sdpMLineIndex = 1
                    )
                )
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun createOffer(): Result<SessionDescriptionWrapper.SessionDescriptionValue> {
        if (withError) {
            throw (Exception("some exception in WebRtcManager"))
        }
        println("create offer")
        return Result.Success(SessionDescriptionWrapper.SessionDescriptionValue("local session description"))
    }

    override suspend fun setLocalDescription(sessionDescription: SessionDescriptionWrapper): Result<Unit> {
        println("set local description")
        return Result.Success(Unit)
    }

    override suspend fun setRemoteDescription(sessionDescription: SessionDescriptionWrapper): Result<Unit> {
        println("set remote description")
        return Result.Success(Unit)
    }

    override suspend fun addRemoteIceCandidates(remoteIceCandidates: List<RemoteIceCandidate>): Result<Unit> {
        println("add remote ice candidate")
        return Result.Success(Unit)
    }

    override fun getDataChannel(): DataChannel {
        return DataChannel(1L)
    }
}
