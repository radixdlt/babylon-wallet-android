package rdx.works.peerdroid.data.webrtc

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import org.webrtc.DataChannel
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.addSuspendingIceCandidate
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.createPeerConnectionFlow
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.createSuspendingAnswer
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.createSuspendingOffer
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.setSuspendingLocalDescription
import rdx.works.peerdroid.data.webrtc.wrappers.peerconnection.setSuspendingRemoteDescription
import rdx.works.peerdroid.helpers.Result
import timber.log.Timber

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

internal class WebRtcManager(applicationContext: Context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PeerConnectionFactoryEntryPoint {
        fun providePeerConnectionFactory(): PeerConnectionFactory
    }

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
    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    private val rtcConfiguration = PeerConnection.RTCConfiguration(
        listOf(stunUrls, turnUrls)
    ).apply {
        iceServers = listOf(stunUrls, turnUrls)
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
    }

    // configuration for the RTCDataChannel
    private val dataChannelInit = DataChannel.Init().apply {
        negotiated = true
        ordered = true
        id = 0
    }

    private val peerConnectionFactoryEntryPoint = EntryPointAccessors.fromApplication(
        context = applicationContext,
        entryPoint = PeerConnectionFactoryEntryPoint::class.java
    )

    private lateinit var peerConnection: PeerConnection
    private lateinit var dataChannel: DataChannel // this will be returned

    init {
        Timber.d("🔌 initialize WebRtcManager")
    }

    fun createPeerConnection(remoteClientId: String): Flow<PeerConnectionEvent> {
        val peerConnectionFactory = peerConnectionFactoryEntryPoint.providePeerConnectionFactory()
        return peerConnectionFactory.createPeerConnectionFlow(
            rtcConfiguration = rtcConfiguration,
            initializePeerConnection = { peerConnection ->
                initializePeerConnection(peerConnection, remoteClientId)
            },
            createRtcDataChannel = { createRtcDataChannel(remoteClientId) },
            remoteClientId = remoteClientId
        )
    }

    private fun initializePeerConnection(peerConnection: PeerConnection?, remoteClientId: String): PeerConnection {
        peerConnection?.let {
            this.peerConnection = it
            Timber.d("🔌 created a peer connection: $peerConnection for remote client: $remoteClientId")
        } ?: Timber.e("🔌 failed to create a peer connection")
        return this.peerConnection
    }

    private fun createRtcDataChannel(remoteClientId: String) {
        peerConnection.let {
            dataChannel = it.createDataChannel(remoteClientId, dataChannelInit)
            Timber.d("🔌 created a data channel for remote client: $remoteClientId")
            Timber.d("🔌 created a RTC data channel")
        }
    }

    suspend fun createOffer(): Result<SessionDescriptionWrapper.SessionDescriptionValue> =
        peerConnection.createSuspendingOffer(mediaConstraints = mediaConstraints)

    suspend fun createAnswer(): Result<SessionDescriptionWrapper.SessionDescriptionValue> =
        peerConnection.createSuspendingAnswer(mediaConstraints = mediaConstraints)

    suspend fun setLocalDescription(
        sessionDescription: SessionDescriptionWrapper
    ): Result<Unit> = peerConnection.setSuspendingLocalDescription(sessionDescription = sessionDescription)

    suspend fun setRemoteDescription(
        sessionDescription: SessionDescriptionWrapper
    ): Result<Unit> = peerConnection.setSuspendingRemoteDescription(sessionDescription = sessionDescription)

    suspend fun addRemoteIceCandidate(remoteIceCandidate: RemoteIceCandidate): Result<Unit> {
        return when (val result = peerConnection.addSuspendingIceCandidate(remoteIceCandidate = remoteIceCandidate)) {
            is Result.Success -> {
                Timber.d("🔌 added successfully ice candidate")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.e("🔌 failed to add ice candidate with error: ${result.message}")
                Result.Error("failed to add ice candidate")
            }
        }
    }

    fun close() {
        Timber.d("🔌 close data channel and peer connection")
        dataChannel.close()
        peerConnection.close()
    }

    fun getDataChannel(): DataChannel = dataChannel
}
