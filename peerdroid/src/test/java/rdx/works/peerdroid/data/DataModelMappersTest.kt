package rdx.works.peerdroid.data

import okio.ByteString.Companion.decodeHex
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import rdx.works.peerdroid.data.PackageMessageDto.Companion.toChunk
import rdx.works.peerdroid.data.PackageMessageDto.Companion.toMetadata
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate.Companion.toWebRtcIceCandidate
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.Companion.toWebRtcSessionDescription
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import rdx.works.peerdroid.data.websocket.model.RpcMessage.IceCandidatePayload.Companion.toJsonArrayPayload
import rdx.works.peerdroid.helpers.toHexString
import rdx.works.peerdroid.domain.BasePackage
import kotlin.test.Test
import kotlin.test.assertEquals

class DataModelMappersTest {

    @Test
    fun `assert mapping a SessionDescriptionWrapper object of type answer to WebRTC SessionDescription is correct`() {
        val expected = SessionDescription(SessionDescription.Type.ANSWER, "description")

        val sessionDescriptionWrapper = SessionDescriptionWrapper(
            type = SessionDescriptionWrapper.Type.ANSWER,
            sessionDescriptionValue = SessionDescriptionValue("description")
        )

        val actual = sessionDescriptionWrapper.toWebRtcSessionDescription()

        assertEquals(expected.type, actual.type)
        assertEquals(expected.description, actual.description)
    }

    @Test
    fun `assert mapping a SessionDescriptionWrapper object of type offer to WebRTC SessionDescription is correct`() {
        val expected = SessionDescription(SessionDescription.Type.OFFER, "description")

        val sessionDescriptionWrapper = SessionDescriptionWrapper(
            type = SessionDescriptionWrapper.Type.OFFER,
            sessionDescriptionValue = SessionDescriptionValue("description")
        )

        val actual = sessionDescriptionWrapper.toWebRtcSessionDescription()

        assertEquals(expected.type, actual.type)
        assertEquals(expected.description, actual.description)
    }

    @Test
    fun `assert mapping a RemoteIceCandidate object to WebRTC IceCandidate is correct`() {
        val expected = IceCandidate("sdpMid", 1, "sdp")

        val remoteIceCandidate = RemoteIceCandidate(
            sdpMid = "sdpMid",
            sdpMLineIndex = 1,
            candidate = "sdp"
        )

        val actual = remoteIceCandidate.toWebRtcIceCandidate()

        assertEquals(expected.sdpMid, actual.sdpMid)
        assertEquals(expected.sdpMLineIndex, actual.sdpMLineIndex)
        assertEquals(expected.sdp, actual.sdp)
    }

    @Test
    fun `assert mapping a list of IceCandidateData to json array payload is correct`() {
        val expected = "[{\"candidate\":\"sdp1\",\"sdpMid\":\"sdpMid1\",\"sdpMLineIndex\":1}, {\"candidate\":\"sdp2\",\"sdpMid\":\"sdpMid2\",\"sdpMLineIndex\":2}]"

        val listOfIceCandidateData = listOf(
            PeerConnectionEvent.IceCandidate.Data(
                candidate = "sdp1",
                sdpMid = "sdpMid1",
                sdpMLineIndex = 1
            ),
            PeerConnectionEvent.IceCandidate.Data(
                candidate = "sdp2",
                sdpMid = "sdpMid2",
                sdpMLineIndex = 2
            )
        )

        val actual = listOfIceCandidateData.toJsonArrayPayload()

        assertEquals(actual.toString(), expected)
    }

    private val hashInHex = "6ea80ead36e3fc4f1ad75134776c26534e73086e93f6b3cd7fdbbe390ed428b5c2f0150fd3f16c928e968497060b39ec61660704"
    @Test
    fun `assert mapping a PackageMessageDto object to MetadataPackage is correct`() {
        val expected = BasePackage.MetadataPackage(
            messageId = "messageId",
            chunkCount = 11,
            hashOfMessage = hashInHex.decodeHex().toByteArray(),
            messageByteCount = 9
        )

        val packageMessageDto = PackageMessageDto(
            packageType = PackageMessageDto.PackageType.METADATA.type,
            messageId = "messageId",
            chunkCount = 11,
            hashOfMessage = hashInHex,
            messageByteCount = 9
        )

        val actual = packageMessageDto.toMetadata()

        assertEquals(expected.messageId, actual.messageId)
        assertEquals(expected.chunkCount, actual.chunkCount)
        assertEquals(expected.hashOfMessage.toHexString(), actual.hashOfMessage.toHexString())
        assertEquals(expected.messageByteCount, actual.messageByteCount)
    }

    @Test
    fun `assert mapping a PackageMessageDto object to Chunk is correct`() {
        val expected = BasePackage.ChunkPackage(
            messageId = "messageId",
            chunkIndex = 11,
            chunkData = "chunk data"
        )

        val packageMessageDto = PackageMessageDto(
            packageType = PackageMessageDto.PackageType.METADATA.type,
            messageId = "messageId",
            chunkIndex = 11,
            chunkData = "chunk data"
        )

        val actual = packageMessageDto.toChunk()

        assertEquals(expected.messageId, actual.messageId)
        assertEquals(expected.chunkIndex, actual.chunkIndex)
        assertEquals(expected.chunkData, actual.chunkData)
    }
}
