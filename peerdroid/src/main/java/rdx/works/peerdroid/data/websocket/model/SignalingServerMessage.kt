package rdx.works.peerdroid.data.websocket.model

import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate

// this is actually a wrapper of the raw SignalingServerMessage
internal sealed interface SignalingServerMessage {

    data class Confirmation(
        val requestId: String
    ) : SignalingServerMessage

    sealed interface RemoteInfo : SignalingServerMessage {

        // remoteClientIsAlreadyConnected or remoteClientJustConnected
        data class ClientConnected(
            val remoteClientId: String
        ) : RemoteInfo

        data class ClientDisconnected(
            val remoteClientId: String
        ) : RemoteInfo

        data class MissingClient(
            val requestId: String
        ) : RemoteInfo
    }

    sealed interface RemoteData : SignalingServerMessage {

        // when the incoming message is an offer from browser extension
        // then return the request id and the remote session description
        data class Offer(
            val targetClientId: String,
            val requestId: String,
            val sdp: String
        ) : RemoteData

        // when the incoming message is an answer from browser extension
        // then return the request id and the remote session description
        data class Answer(
            val targetClientId: String,
            val requestId: String,
            val sdp: String
        ) : RemoteData

        // when the incoming message is an ice candidate from browser extension
        // then return the request id and the remote ice candidate
        data class IceCandidate(
            val targetClientId: String,
            val requestId: String,
            val remoteIceCandidate: RemoteIceCandidate
        ) : RemoteData
    }

    sealed interface Error : SignalingServerMessage {

        data class InvalidMessage(
            val errorMessage: String
        ) : SignalingServerMessage

        object Validation : SignalingServerMessage

        object Unknown : SignalingServerMessage
    }
}
