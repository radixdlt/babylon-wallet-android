package rdx.works.peerdroid.data.webrtc

import android.util.Log
import kotlinx.coroutines.flow.Flow
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.addSuspendingIceCandidate
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.createPeerConnectionFlow
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.createSuspendingOffer
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.setSuspendingLocalDescription
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.setSuspendingRemoteDescription
import rdx.works.peerdroid.helpers.Result
import javax.inject.Inject
import javax.inject.Singleton

private val STUN_SERVERS_LIST = listOf(
    "stun:stun.l.google.com:19302",
    "stun:stun1.l.google.com:19302",
    "stun:stun2.l.google.com:19302",
    "stun:stun3.l.google.com:19302",
    "stun:stun4.l.google.com:19302"
)
private val TURN_SERVERS_LIST = listOf(
    "turn:turn-dev-tcp.rdx-works-main.extratools.works:80",
    "turn:turn-dev-udp.rdx-works-main.extratools.works:80"
)
private const val TURN_SERVER_USERNAME = "username"
private const val TURN_SERVER_PASSWORD = "password"

internal interface WebRtcManager {

    fun createPeerConnection(): Flow<PeerConnectionEvent>

    suspend fun createOffer(): Result<SessionDescriptionValue>

    suspend fun setLocalDescription(sessionDescription: SessionDescriptionWrapper): Result<Unit>

    suspend fun setRemoteDescription(sessionDescription: SessionDescriptionWrapper): Result<Unit>

    suspend fun addRemoteIceCandidates(remoteIceCandidates: List<RemoteIceCandidate>): Result<Unit>

    fun getDataChannel(): DataChannel
}

/*
 * WebRtcManager flow in summary:
 * 1. create peer connection
 * 2. create data channel
 * 3. create offer
 * 4. set local description
 * 5. generate/discover local ICE Candidates
 * 6. set remote Answer (remote description)
 * 7. add remote ice candidates => return data channel
 *
 */
@Singleton
internal class WebRtcManagerImpl @Inject constructor(
    private val peerConnectionFactory: PeerConnectionFactory
) : WebRtcManager {
    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel // this will be returned

    // STUN servers are used to find the public facing IP address of each peer
    private val stunUrls = PeerConnection
        .IceServer
        .builder(STUN_SERVERS_LIST)
        .createIceServer()

    // if STUN servers fail, then a TURN server is used instead as a proxy fallback
    private val turnUrls = PeerConnection
        .IceServer
        .builder(TURN_SERVERS_LIST)
        .setUsername(TURN_SERVER_USERNAME)
        .setPassword(TURN_SERVER_PASSWORD)
        .createIceServer()

    // DtlsSrtpKeyAgreement is for peer connection and IceRestart for offer.
    // Check the WebRTCImplementation+CreatePeerConnection.swift
    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    private val rtcConfiguration = PeerConnection.RTCConfiguration(
        listOf(stunUrls, turnUrls)
    ).apply {
        iceServers = listOf(stunUrls, turnUrls)
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
    }

    // configuration for the RTCDataChannel
    private val dataChannelInit = DataChannel.Init().apply {
        negotiated = true
        ordered = true
        id = 0
    }

    init {
        Log.d("WEB_RTC", "initialize WebRTC manager")
    }

    override fun createPeerConnection(): Flow<PeerConnectionEvent> =
        peerConnectionFactory.createPeerConnectionFlow(
            rtcConfiguration = rtcConfiguration,
            initializePeerConnection = { peerConnection ->
                initializePeerConnection(peerConnection)
            },
            createRtcDataChannel = ::createRtcDataChannel
        )

    private fun initializePeerConnection(peerConnection: PeerConnection?) {
        peerConnection?.let {
            this.peerConnection = it
            Log.d("WEB_RTC", "created a peer connection")
        } ?: Log.e("WEB_RTC", "failed to create a peer connection")
    }

    private fun createRtcDataChannel() {
        dataChannel = peerConnection.createDataChannel("peerdroid data channel", dataChannelInit)
        Log.d("WEB_RTC", "created a RTC data channel")
    }

    override suspend fun createOffer(): Result<SessionDescriptionValue> =
        peerConnection.createSuspendingOffer(mediaConstraints = mediaConstraints)

    override suspend fun setLocalDescription(
        sessionDescription: SessionDescriptionWrapper
    ): Result<Unit> = peerConnection.setSuspendingLocalDescription(sessionDescription = sessionDescription)

    override suspend fun setRemoteDescription(
        sessionDescription: SessionDescriptionWrapper
    ): Result<Unit> = peerConnection.setSuspendingRemoteDescription(sessionDescription = sessionDescription)

    // this is the last step in the flow,
    // webrtc adds all remote ice candidates successfully
    override suspend fun addRemoteIceCandidates(
        remoteIceCandidates: List<RemoteIceCandidate>
    ): Result<Unit> {
        var areAllIceCandidatesAdded = true

        remoteIceCandidates.forEach { remoteIceCandidate ->
            areAllIceCandidatesAdded = areAllIceCandidatesAdded.and(
                peerConnection.addSuspendingIceCandidate(remoteIceCandidate = remoteIceCandidate)
            )
        }

        return if (areAllIceCandidatesAdded) {
            Result.Success(Unit)
        } else {
            Result.Error("failed to add all ice candidates")
        }
    }

    override fun getDataChannel(): DataChannel = dataChannel
}
