package rdx.works.peerdroid.data.websocket.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import rdx.works.core.UUIDGenerator
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.SessionDescriptionWrapper

/**
 * Before mobile wallet clients can communicate directly with the browser extensions over WebRTC
 * they exchange setup-WebRTC-messages over Websockets via the signaling server.
 *
 * This data class is an implementation of only the websocket messages
 * that originate from the browser extension or from the Mobile wallet.
 *
 * This data class is not used for the notification messages
 * originating from the Signaling Server itself.
 * These are encapsulated in the [SignalingServerMessage].
 *
 *
 * @param method               describes the RPC method, offer, answer, or ice candidate
 * @param targetClientId       used by the Signalling Server to determine to which dapp to route the RpcMessage
 * @param encryptedPayload     contains the payload of the RPC method, an offer or an ice candidate
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("method")
internal sealed class RpcMessage {

    @Serializable
    @SerialName("offer")
    data class Offer(
        @SerialName("requestId")
        val requestId: String = UUIDGenerator.uuid().toString(),
        @SerialName("targetClientId")
        val targetClientId: String,
        @SerialName("encryptedPayload")
        val encryptedPayload: String
    ) : RpcMessage()

    @Serializable
    @SerialName("answer")
    data class Answer(
        @SerialName("requestId")
        val requestId: String = UUIDGenerator.uuid().toString(),
        @SerialName("targetClientId")
        val targetClientId: String,
        @SerialName("encryptedPayload")
        val encryptedPayload: String
    ) : RpcMessage()

    @Serializable
    @SerialName("iceCandidate")
    data class IceCandidate(
        @SerialName("requestId")
        val requestId: String = UUIDGenerator.uuid().toString(),
        @SerialName("targetClientId")
        val targetClientId: String,
        @SerialName("encryptedPayload")
        val encryptedPayload: String
    ) : RpcMessage()

    @Serializable
    data class OfferPayload(
        @SerialName("sdp")
        val sdp: String
    ) {

        companion object {
            fun SessionDescriptionWrapper.SessionDescriptionValue.toOfferPayload() = OfferPayload(
                sdp = sdp
            )
        }
    }

    @Serializable
    data class AnswerPayload(
        @SerialName("sdp")
        val sdp: String
    ) {

        companion object {
            fun SessionDescriptionWrapper.SessionDescriptionValue.toAnswerPayload() = AnswerPayload(
                sdp = sdp
            )
        }
    }

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
