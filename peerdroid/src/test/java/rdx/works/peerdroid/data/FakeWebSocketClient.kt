package rdx.works.peerdroid.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.JsonElement
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.websocket.WebSocketClient
import rdx.works.peerdroid.data.websocket.model.RpcMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerIncomingMessage
import rdx.works.peerdroid.di.IoDispatcher
import rdx.works.peerdroid.helpers.Result
import javax.inject.Inject

internal class FakeWebSocketClient @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val withError: Boolean = false
) : WebSocketClient {

    override suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit> {
        println("initSession")
        return Result.Success(Unit)
    }

    override suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload) {
        if (withError) {
            throw (Exception("some exception in WebSocketClient"))
        }
        println("send new offer")
    }

    override suspend fun sendIceCandidatesMessage(iceCandidatePayload: List<JsonElement>) {
        println("send new ice candidates")
    }

    override suspend fun sendMessage(message: String) {
        println("send message")
    }

    override fun observeMessages(): Flow<SignalingServerIncomingMessage> {
        return flow {
            emit(SignalingServerIncomingMessage.Confirmation("offer request id"))
            delay(100)
            emit(SignalingServerIncomingMessage.BrowserExtensionAnswer("answer request id", "sdp"))
            delay(100)
            emit(
                SignalingServerIncomingMessage.BrowserExtensionIceCandidates(
                    "ice candidates request id",
                    listOf(
                        RemoteIceCandidate(
                            sdpMid = "sdpMid",
                            sdpMLineIndex = 1,
                            candidate = "candidate"
                        )
                    )
                )
            )
        }.flowOn(ioDispatcher)
    }

    override suspend fun closeSession() {
    }
}
