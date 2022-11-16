package rdx.works.peerdroid.data.websocket

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.ByteString.Companion.decodeHex
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.websocket.model.RpcMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerIncomingMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerResponse
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.helpers.decryptWithAes
import rdx.works.peerdroid.helpers.encryptWithAes
import rdx.works.peerdroid.helpers.sha256
import rdx.works.peerdroid.helpers.toHexString

internal interface WebSocketClient {

    suspend fun initSession(encryptionKey: ByteArray): Result<Unit>

    suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload)

    suspend fun sendIceCandidatesMessage(iceCandidatePayload: List<JsonElement>)

    suspend fun sendMessage(message: String)

    fun observeMessages(): Flow<SignalingServerIncomingMessage>

    suspend fun closeSession()
}

// WebSocket client to communicate with the signaling server.
// The signaling server is responsible for exchanging network information which is needed for the WebRTC
// between the mobile wallet and the browser extension.
internal class WebSocketClientImpl(
    private val client: HttpClient
) : WebSocketClient {

    // represents a web socket session between two peers
    private var socket: WebSocketSession? = null
    private lateinit var connectionId: String
    private lateinit var encryptionKey: ByteArray

    override suspend fun initSession(encryptionKey: ByteArray): Result<Unit> {
        return try {
            this.encryptionKey = encryptionKey
            this.connectionId = encryptionKey.sha256().toHexString()

            // this block has a normal http request builder
            // because we need to do an http request once (initial handshake)
            // to establish the connection the first time
            socket = client.webSocketSession {
                url("$BASE_URL$connectionId?source=wallet&target=extension")
            }
            if (socket?.isActive == true) {
                Log.d("WEB_SOCKET", "successfully connected to signaling server")
                Result.Success(Unit)
            } else {
                Log.d("WEB_SOCKET", "failed to connect to signaling server")
                Result.Error("Couldn't establish a connection.")
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Log.d("WEB_SOCKET", "connection exception: ${exception.printStackTrace()}")
            Result.Error(exception.localizedMessage ?: "Unknown error")
        }
    }

    override suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload) {
        val offerJson = Json.encodeToString(offerPayload)
        val encryptedOffer = encryptWithAes(
            input = offerJson.toByteArray(),
            encryptionKey = encryptionKey
        )

        val rpcMessage = RpcMessage(
            method = RpcMessage.RpcMethod.OFFER.value,
            source = RpcMessage.ClientSource.MOBILE_WALLET.value,
            connectionId = connectionId,
            encryptedPayload = encryptedOffer.toHexString()
        )

        val message = Json.encodeToString(rpcMessage)
        Log.d("WEB_SOCKET", "=> sending offer with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override suspend fun sendIceCandidatesMessage(iceCandidatePayload: List<JsonElement>) {
        val encryptedIceCandidates = encryptWithAes(
            input = iceCandidatePayload.toString().toByteArray(),
            encryptionKey = encryptionKey
        )
        val rpcMessage = RpcMessage(
            method = RpcMessage.RpcMethod.ICE_CANDIDATES.value,
            source = RpcMessage.ClientSource.MOBILE_WALLET.value,
            connectionId = connectionId,
            encryptedPayload = encryptedIceCandidates.toHexString()
        )

        val message = Json.encodeToString(rpcMessage)
        Log.d("WEB_SOCKET", "=> sending ice candidates with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override suspend fun sendMessage(message: String) {
        try {
            socket?.send(Frame.Text(message))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Log.d("WEB_SOCKET", "failed to send message")
            exception.printStackTrace()
        }
    }

    override fun observeMessages(): Flow<SignalingServerIncomingMessage> {
        return try {
            socket?.incoming // web socket channel
                ?.receiveAsFlow()
                ?.filterIsInstance<Frame.Text>()
                ?.mapNotNull {
                    val responseString = it.readText()
                    val responseJson = decodeJsonFromString(responseString)
                    if (responseJson != null) {
                        val response = parseResponseFromJson(responseJson)
                        Log.d("WEB_SOCKET", "<= received message from signaling server")
                        response
                    } else {
                        Log.e("WEB_SOCKET", "<= received message from signaling server but failed to parse it")
                        SignalingServerIncomingMessage.ParsingResponseError
                    }
                } ?: flowOf(SignalingServerIncomingMessage.UnknownError)
        } catch (exception: Exception) {
            Log.d("WEB_SOCKET", "incoming message exception")
            exception.printStackTrace()
            flowOf(SignalingServerIncomingMessage.UnknownError)
        }
    }

    override suspend fun closeSession() {
        socket?.close()
    }

    private fun decodeJsonFromString(response: String): SignalingServerResponse? {
        return try {
            Json.decodeFromString(response)
        } catch (se: SerializationException) {
            Log.e("WEB_SOCKET", "decoding signaling server response json failed: ${se.localizedMessage}")
            null
        }
    }

    private fun parseResponseFromJson(responseJson: SignalingServerResponse): SignalingServerIncomingMessage {
        // based on the info of the response return the corresponding SignalingServerIncomingMessage data model
        // if info is remoteData then encapsulate the encrypted payload in the SignalingServerIncomingMessage data model
        return when (SignalingServerResponse.Info.from(responseJson.info)) {

            SignalingServerResponse.Info.CONFIRMATION -> SignalingServerIncomingMessage.Confirmation(
                requestId = responseJson.requestId ?: ""
            )

            SignalingServerResponse.Info.DATA_FROM_BROWSER_EXTENSION -> parseRemoteDataFromResponse(responseJson)

            SignalingServerResponse.Info.REMOTE_CLIENT_DISCONNECTED -> SignalingServerIncomingMessage.RemoteClientDisconnected

            SignalingServerResponse.Info.REMOTE_CLIENT_IS_ALREADY_CONNECTED -> SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected

            SignalingServerResponse.Info.REMOTE_CLIENT_JUST_CONNECTED -> SignalingServerIncomingMessage.RemoteClientJustConnected

            SignalingServerResponse.Info.MISSING_REMOTE_CLIENT_ERROR -> SignalingServerIncomingMessage.MissingRemoteClientError(
                requestId = responseJson.requestId ?: ""
            )

            SignalingServerResponse.Info.INVALID_MESSAGE_ERROR -> SignalingServerIncomingMessage.InvalidMessageError(
                errorMessage = responseJson.error ?: "unknown error"
            )

            SignalingServerResponse.Info.VALIDATION_ERROR -> SignalingServerIncomingMessage.ValidationError
        }
    }

    private fun parseRemoteDataFromResponse(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage {
        // check if the request id and payload is not null, otherwise return an UnknownMessage
        return if (responseJson.requestId != null && responseJson.data != null) {
            // check if client's connection id is equal to extension's connection id, and if not return an error
            if (connectionId != responseJson.data.connectionId) {
                SignalingServerIncomingMessage.RemoteConnectionIdNotMatchedError
            }
            // check if remote's source is "extension", and if not return an error
            if (responseJson.data.source != RpcMessage.ClientSource.BROWSER_EXTENSION.value) {
                SignalingServerIncomingMessage.RemoteClientSourceError
            }
            // Check whether the method is "answer" or "iceCandidates" and build the
            // corresponding SignalingServerIncomingMessage data model.
            // We do not handle the "offer" because the wallet initiates the WebRTC communication,
            // therefore in case an "offer" is received we return a UnknownMessage.
            when (RpcMessage.RpcMethod.from(responseJson.data.method)) {
                RpcMessage.RpcMethod.ANSWER -> {
                    decryptAndParseAnswerPayload(responseJson)
                }
                RpcMessage.RpcMethod.ICE_CANDIDATES -> {
                    decryptAndParseIceCandidatesPayload(responseJson)
                }
                RpcMessage.RpcMethod.OFFER -> {
                    SignalingServerIncomingMessage.UnknownMessage
                }
            }
        } else {
            SignalingServerIncomingMessage.UnknownMessage
        }
    }

    private fun decryptAndParseAnswerPayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionAnswer {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in answer payload")
        }

        val message = decryptWithAes(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val answer = Json.decodeFromString<RpcMessage.AnswerPayload>(message)

        return SignalingServerIncomingMessage.BrowserExtensionAnswer(
            requestId = responseJson.requestId,
            sdp = answer.sdp
        )
    }

    private fun decryptAndParseIceCandidatesPayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionIceCandidates {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in remote ice candidates payload")
        }

        val message = decryptWithAes(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val iceCandidates = Json.decodeFromString<List<RpcMessage.IceCandidatePayload>>(message)

        val remoteIceCandidates = iceCandidates.map { iceCandidatePayload ->
            RemoteIceCandidate(
                sdpMid = iceCandidatePayload.sdpMid,
                sdpMLineIndex = iceCandidatePayload.sdpMLineIndex,
                candidate = iceCandidatePayload.candidate
            )
        }

        return SignalingServerIncomingMessage.BrowserExtensionIceCandidates(
            requestId = responseJson.requestId,
            remoteIceCandidates = remoteIceCandidates
        )
    }

    companion object {
        // TODO same url for production?
        private const val BASE_URL = "wss://signaling-server-dev.rdx-works-main.extratools.works/"
    }
}
