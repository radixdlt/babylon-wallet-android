package rdx.works.peerdroid.data.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okio.ByteString.Companion.decodeHex
import rdx.works.core.decryptData
import rdx.works.core.encryptData
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.websocket.model.RpcMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerIncomingMessage
import rdx.works.peerdroid.data.websocket.model.SignalingServerResponse
import rdx.works.peerdroid.helpers.Result
import rdx.works.peerdroid.helpers.toHexString
import timber.log.Timber
import java.nio.charset.StandardCharsets

internal interface WebSocketClient {

    suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit>

    suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload)

    suspend fun sendIceCandidatesMessage(iceCandidatePayload: List<JsonElement>)

    suspend fun sendMessage(message: String)

    fun observeMessages(): Flow<SignalingServerIncomingMessage>

    suspend fun closeSession()
}

@Suppress("TooManyFunctions")
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

    override suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit> {
        return try {
            this.connectionId = connectionId
            this.encryptionKey = encryptionKey

            // this block has a normal http request builder
            // because we need to do an http request once (initial handshake)
            // to establish the connection the first time
            socket = client.webSocketSession {
                url("$BASE_URL$connectionId?source=wallet&target=extension")
            }
            if (socket?.isActive == true) {
                Timber.d("successfully connected to signaling server")
                Timber.d("waiting remote peer to connect to signaling server")
                waitUntilRemotePeerIsConnected()
                Result.Success(Unit)
            } else {
                Timber.d("failed to connect to signaling server")
                Result.Error("Couldn't establish a connection.")
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("connection exception: ${exception.localizedMessage}")
            Result.Error(exception.localizedMessage ?: "Unknown error")
        }
    }

    private suspend fun waitUntilRemotePeerIsConnected() {
        socket?.incoming
            ?.receiveAsFlow()
            ?.filterIsInstance<Frame.Text>()
            ?.mapNotNull { frameText ->
                val responseJsonString = frameText.readText()
                decodeAndParseResponseFromJson(responseJsonString)
            }
            ?.takeWhile { signalingServerIncomingMessage ->
                signalingServerIncomingMessage != SignalingServerIncomingMessage.RemoteClientJustConnected &&
                    signalingServerIncomingMessage != SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected
            }
            ?.collect()
    }

    override suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload) {
        val offerJson = Json.encodeToString(offerPayload)
        val encryptedOffer = encryptData(
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
        Timber.d("=> sending offer with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override suspend fun sendIceCandidatesMessage(iceCandidatePayload: List<JsonElement>) {
        val encryptedIceCandidates = encryptData(
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
        Timber.d("=> sending ice candidates with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override suspend fun sendMessage(message: String) {
        try {
            socket?.send(Frame.Text(message))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.d("failed to send message: ${exception.localizedMessage}")
        }
    }

    override fun observeMessages(): Flow<SignalingServerIncomingMessage> {
        return try {
            socket?.incoming // web socket channel
                ?.receiveAsFlow()
                ?.filterIsInstance<Frame.Text>()
                ?.mapNotNull { frameText ->
                    val responseJsonString = frameText.readText()
                    decodeAndParseResponseFromJson(responseJsonString)
                }
                ?: flowOf(SignalingServerIncomingMessage.UnknownError)
        } catch (exception: Exception) {
            Timber.e("incoming message exception: ${exception.localizedMessage}")
            flowOf(SignalingServerIncomingMessage.UnknownError)
        }
    }

    override suspend fun closeSession() {
        socket?.close()
    }

    private fun decodeAndParseResponseFromJson(responseJsonString: String): SignalingServerIncomingMessage {
        val responseJson = Json.decodeFromString<SignalingServerResponse>(responseJsonString)
        // based on the info of the response return the corresponding SignalingServerIncomingMessage data model
        // if info is remoteData then encapsulate the encrypted payload in the SignalingServerIncomingMessage data model
        return when (SignalingServerResponse.Info.from(responseJson.info)) {
            SignalingServerResponse.Info.CONFIRMATION -> SignalingServerIncomingMessage.Confirmation(
                requestId = responseJson.requestId.orEmpty()
            )

            SignalingServerResponse.Info.DATA_FROM_BROWSER_EXTENSION -> parseRemoteDataFromResponse(responseJson)

            SignalingServerResponse.Info.REMOTE_CLIENT_DISCONNECTED ->
                SignalingServerIncomingMessage.RemoteClientDisconnected

            SignalingServerResponse.Info.REMOTE_CLIENT_IS_ALREADY_CONNECTED ->
                SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected

            SignalingServerResponse.Info.REMOTE_CLIENT_JUST_CONNECTED ->
                SignalingServerIncomingMessage.RemoteClientJustConnected

            SignalingServerResponse.Info.MISSING_REMOTE_CLIENT_ERROR ->
                SignalingServerIncomingMessage.MissingRemoteClientError(
                    requestId = responseJson.requestId.orEmpty()
                )

            SignalingServerResponse.Info.INVALID_MESSAGE_ERROR -> SignalingServerIncomingMessage.InvalidMessageError(
                errorMessage = responseJson.error ?: "unknown error"
            )

            SignalingServerResponse.Info.VALIDATION_ERROR -> SignalingServerIncomingMessage.ValidationError
        }
    }

    @Suppress("ReturnCount")
    private fun parseRemoteDataFromResponse(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage {
        // check if the request id and payload is not null, otherwise return an UnknownMessage
        if (responseJson.requestId != null && responseJson.data != null) {
            // check if client's connection id is equal to extension's connection id, and if not return an error
            if (connectionId != responseJson.data.connectionId) {
                return SignalingServerIncomingMessage.RemoteConnectionIdNotMatchedError
            }
            // check if remote's source is "extension", and if not return an error
            if (responseJson.data.source != RpcMessage.ClientSource.BROWSER_EXTENSION.value) {
                return SignalingServerIncomingMessage.RemoteClientSourceError
            }
            // Check whether the method is "answer" or "iceCandidates" and build the
            // corresponding SignalingServerIncomingMessage data model.
            // We do not handle the "offer" because the wallet initiates the WebRTC communication,
            // therefore in case an "offer" is received we return a UnknownMessage.
            return when (RpcMessage.RpcMethod.from(responseJson.data.method)) {
                RpcMessage.RpcMethod.ANSWER -> {
                    decryptAndParseAnswerPayload(responseJson)
                }
                RpcMessage.RpcMethod.ICE_CANDIDATE -> {
                    decryptAndParseIceCandidatePayload(responseJson)
                }
                RpcMessage.RpcMethod.ICE_CANDIDATES -> {
                    decryptAndParseIceCandidatesPayload(responseJson)
                }
                RpcMessage.RpcMethod.OFFER -> {
                    SignalingServerIncomingMessage.UnknownMessage
                }
            }
        } else {
            return SignalingServerIncomingMessage.UnknownMessage
        }
    }

    @Suppress("UseRequire")
    private fun decryptAndParseAnswerPayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionAnswer {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in answer payload")
        }

        val message = decryptData(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val answer = Json.decodeFromString<RpcMessage.AnswerPayload>(String(message, StandardCharsets.UTF_8))

        return SignalingServerIncomingMessage.BrowserExtensionAnswer(
            requestId = responseJson.requestId,
            sdp = answer.sdp
        )
    }

    @Suppress("UseRequire")
    private fun decryptAndParseIceCandidatePayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionIceCandidates {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in remote ice candidate payload")
        }

        val message = decryptData(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val iceCandidate = Json.decodeFromString<RpcMessage.IceCandidatePayload>(
            String(message, StandardCharsets.UTF_8)
        )

        val remoteIceCandidates = RemoteIceCandidate(
            sdpMid = iceCandidate.sdpMid,
            sdpMLineIndex = iceCandidate.sdpMLineIndex,
            candidate = iceCandidate.candidate
        )

        // TODO maybe later add a new data class BrowserExtensionIceCandidate to pass only one ice candidate.
        //  At the moment it works very well this way.
        return SignalingServerIncomingMessage.BrowserExtensionIceCandidates(
            requestId = responseJson.requestId,
            remoteIceCandidates = listOf(remoteIceCandidates)
        )
    }

    @Suppress("UseRequire")
    private fun decryptAndParseIceCandidatesPayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionIceCandidates {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in remote ice candidates payload")
        }

        val message = decryptData(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val iceCandidates = Json.decodeFromString<List<RpcMessage.IceCandidatePayload>>(
            String(message, StandardCharsets.UTF_8)
        )

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
//        private const val BASE_URL = "wss://signaling-server-dev.rdx-works-main.extratools.works/"
        private const val BASE_URL = "wss://signaling-server-betanet.radixdlt.com/"
    }
}
