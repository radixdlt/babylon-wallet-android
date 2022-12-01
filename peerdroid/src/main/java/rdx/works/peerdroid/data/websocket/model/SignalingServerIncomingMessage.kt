package rdx.works.peerdroid.data.websocket.model

import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate

// this is actually a wrapper of the raw SignalingServerResponse
internal sealed interface SignalingServerIncomingMessage {

    data class Confirmation(
        val requestId: String
    ) : SignalingServerIncomingMessage

    // when the incoming message is an answer from browser extension
    // then return the request id and the remote session description
    data class BrowserExtensionAnswer(
        val requestId: String,
        val sdp: String
    ) : SignalingServerIncomingMessage

    // when the incoming message is an ice candidates from browser extension
    // then return the request id and the remote ice candidates
    data class BrowserExtensionIceCandidates(
        val requestId: String,
        val remoteIceCandidates: List<RemoteIceCandidate>
    ) : SignalingServerIncomingMessage

    object RemoteClientDisconnected : SignalingServerIncomingMessage

    object RemoteClientIsAlreadyConnected : SignalingServerIncomingMessage

    object RemoteClientJustConnected : SignalingServerIncomingMessage

    data class MissingRemoteClientError(
        val requestId: String
    ) : SignalingServerIncomingMessage

    data class InvalidMessageError(
        val errorMessage: String
    ) : SignalingServerIncomingMessage

    object ValidationError : SignalingServerIncomingMessage

    object UnknownMessage : SignalingServerIncomingMessage

    object UnknownError : SignalingServerIncomingMessage

    object RemoteConnectionIdNotMatchedError : SignalingServerIncomingMessage

    object RemoteClientSourceError : SignalingServerIncomingMessage
}
