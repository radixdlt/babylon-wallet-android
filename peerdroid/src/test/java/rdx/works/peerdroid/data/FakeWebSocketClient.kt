package rdx.works.peerdroid.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result
import javax.inject.Inject

internal class FakeWebSocketClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WebSocketClient {

    override suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit> {
        println("initSession")
        return Result.Success(Unit)
    }

    override suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload) {
        println("send new offer")
    }

    override suspend fun sendAnswerMessage(answerPayload: RpcMessage.AnswerPayload) {
        println("send new answer")
    }

    override suspend fun sendIceCandidateMessage(iceCandidateData: PeerConnectionEvent.IceCandidate.Data) {
        println("send new ice candidate")
    }

    override fun observeMessages(): Flow<SignalingServerMessage> {
        return flow {
            delay(100)
            emit(SignalingServerMessage.RemoteData.Offer("offer request id", "requestId", "sdp"))
            delay(200)
            emit(
                SignalingServerMessage.RemoteData.IceCandidate(
                    targetClientId = "targetClientId",
                    requestId = "ice candidates request id",
                    RemoteIceCandidate(
                        sdpMid = "sdpMid",
                        sdpMLineIndex = 1,
                        candidate = "candidate"
                    )
                )
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun closeSession() {
    }
}
