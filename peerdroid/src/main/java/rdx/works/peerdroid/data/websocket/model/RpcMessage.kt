package rdx.works.peerdroid.data.websocket.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper.SessionDescriptionValue
import java.util.UUID

/**
 * Before mobile wallet clients can communicate directly with the browser extensions over WebRTC
 * they exchange setup-WebRTC-messages over Websockets via the signaling server.
 *
 * This data class is an implementation of only the websocket messages
 * that originate from the browser extension or from the Mobile wallet.
 *
 * This data class is not used for the notification messages
 * originating from the Signaling Server itself.
 * These are encapsulated in the [SignalingServerResponse].
 *
 *
 * @param method               describes the RPC method, offer, answer, or ice candidate
 * @param source               the sender of the message, either wallet or extension
 * @param encryptedPayload     contains the payload of the RPC method, an offer or an ice candidate
 */
@Serializable
internal data class RpcMessage(
    @SerialName("requestId")
    val requestId: String = UUID.randomUUID().toString(),
    @SerialName("method")
    val method: String,
    @SerialName("source")
    val source: String,
    @SerialName("connectionId")
    val connectionId: String,
    @SerialName("encryptedPayload")
    val encryptedPayload: String,
) {

    enum class RpcMethod(val value: String) {
        OFFER("offer"),
        ANSWER("answer"),
        ICE_CANDIDATE("iceCandidate");

        companion object {
            fun from(value: String): RpcMethod = requireNotNull(
                values().find { rpcMethod -> rpcMethod.value == value }
            ) {
                "No RpcMethod with value $value"
            }
        }
    }

    enum class ClientSource(val value: String) {
        BROWSER_EXTENSION("extension"),
        MOBILE_WALLET("wallet")
    }

    @Serializable
    data class OfferPayload(
        @SerialName("sdp")
        val sdp: String
    ) {

        companion object {
            fun SessionDescriptionValue.toPayload() = OfferPayload(
                sdp = sdp
            )
        }
    }

    @Serializable
    data class AnswerPayload(
        @SerialName("sdp")
        val sdp: String
    )

    @Serializable
    data class IceCandidatePayload(
        @SerialName("candidate")
        val candidate: String,
        @SerialName("sdpMid")
        val sdpMid: String,
        @SerialName("sdpMLineIndex")
        val sdpMLineIndex: Int,
    ) {

        companion object {
            fun PeerConnectionEvent.IceCandidate.Data.toJsonPayload(): JsonElement {
                val payload = IceCandidatePayload(
                    candidate = candidate,
                    sdpMid = sdpMid,
                    sdpMLineIndex = sdpMLineIndex
                )
                return Json.encodeToJsonElement(payload)
            }
        }
    }
}
