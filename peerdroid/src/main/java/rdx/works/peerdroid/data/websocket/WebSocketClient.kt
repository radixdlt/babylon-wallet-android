package rdx.works.peerdroid.data.websocket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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
import rdx.works.peerdroid.di.ApplicationScope
import rdx.works.peerdroid.di.IoDispatcher
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

    suspend fun sendIceCandidateMessage(iceCandidatePayload: JsonElement)

    fun observeMessages(): Flow<SignalingServerIncomingMessage>

    suspend fun closeSession()
}

@Suppress("TooManyFunctions")
// WebSocket client to communicate with the signaling server.
// The signaling server is responsible for exchanging network information which is needed for the WebRTC
// between the mobile wallet and the browser extension.
internal class WebSocketClientImpl(
    private val client: HttpClient,
    private val json: Json,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : WebSocketClient {

    // represents a web socket session between two peers
    private var socket: WebSocketSession? = null

    private lateinit var connectionId: String
    private lateinit var encryptionKey: ByteArray

    private lateinit var sessionDeferred: CompletableDeferred<Result<Unit>>

    override suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit> {
        sessionDeferred = CompletableDeferred()
        try {
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
            } else {
                Timber.e("failed to connect to signaling server")
                sessionDeferred.complete(Result.Error("Couldn't establish a connection."))
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("connection exception: ${exception.localizedMessage}")
            sessionDeferred.complete(Result.Error(exception.localizedMessage ?: "Unknown error"))
        }

        return sessionDeferred.await()
    }

    private fun waitUntilRemotePeerIsConnected() {
        socket?.incoming
            ?.receiveAsFlow()
            ?.onStart { // for debugging
                Timber.d("start observing remote peer connection until is connected")
            }
            ?.onCompletion { // for debugging
                Timber.d("end observing remote peer connection until is connected")
            }
            ?.filterIsInstance<Frame.Text>()
            ?.mapNotNull { frameText ->
                val responseJsonString = frameText.readText()
                decodeAndParseResponseFromJson(responseJsonString)
            }
            ?.takeWhile { signalingServerIncomingMessage ->
                sessionDeferred.complete(Result.Success(Unit))
                signalingServerIncomingMessage != SignalingServerIncomingMessage.RemoteClientJustConnected &&
                    signalingServerIncomingMessage != SignalingServerIncomingMessage.RemoteClientIsAlreadyConnected
            }
            ?.flowOn(ioDispatcher)
            ?.cancellable()
            ?.launchIn(applicationScope)
    }

    override suspend fun sendOfferMessage(offerPayload: RpcMessage.OfferPayload) {
        val offerJson = json.encodeToString(offerPayload)
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

        val message = json.encodeToString(rpcMessage)
        Timber.d("=> sending offer with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override suspend fun sendIceCandidateMessage(iceCandidatePayload: JsonElement) {
        val encryptedIceCandidate = encryptData(
            input = iceCandidatePayload.toString().toByteArray(),
            encryptionKey = encryptionKey
        )
        val rpcMessage = RpcMessage(
            method = RpcMessage.RpcMethod.ICE_CANDIDATE.value,
            source = RpcMessage.ClientSource.MOBILE_WALLET.value,
            connectionId = connectionId,
            encryptedPayload = encryptedIceCandidate.toHexString()
        )

        val message = json.encodeToString(rpcMessage)
        Timber.d("=> sending ice candidate with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    override fun observeMessages(): Flow<SignalingServerIncomingMessage> {
        return try {
            socket?.incoming // web socket channel
                ?.receiveAsFlow()
                ?.cancellable()
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

    private suspend fun sendMessage(message: String) {
        try {
            socket?.send(Frame.Text(message))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("failed to send message: ${exception.localizedMessage}")
        }
    }

    private fun decodeAndParseResponseFromJson(responseJsonString: String): SignalingServerIncomingMessage {
        val responseJson = json.decodeFromString<SignalingServerResponse>(responseJsonString)
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
            // Check whether the method is "answer" or "iceCandidate" and build the
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
        val answer = json.decodeFromString<RpcMessage.AnswerPayload>(String(message, StandardCharsets.UTF_8))

        return SignalingServerIncomingMessage.BrowserExtensionAnswer(
            requestId = responseJson.requestId,
            sdp = answer.sdp
        )
    }

    @Suppress("UseRequire")
    private fun decryptAndParseIceCandidatePayload(
        responseJson: SignalingServerResponse
    ): SignalingServerIncomingMessage.BrowserExtensionIceCandidate {
        if (responseJson.data == null || responseJson.requestId.isNullOrEmpty()) {
            // should never reach this point!
            throw IllegalArgumentException("rpc message is null in remote ice candidate payload")
        }

        val message = decryptData(
            input = responseJson.data.encryptedPayload.decodeHex().toByteArray(),
            encryptionKey = encryptionKey
        )
        val iceCandidateString = json.decodeFromString<RpcMessage.IceCandidatePayload>(
            String(message, StandardCharsets.UTF_8)
        )

        val remoteIceCandidate = RemoteIceCandidate(
            sdpMid = iceCandidateString.sdpMid,
            sdpMLineIndex = iceCandidateString.sdpMLineIndex,
            candidate = iceCandidateString.candidate
        )

        return SignalingServerIncomingMessage.BrowserExtensionIceCandidate(
            requestId = responseJson.requestId,
            remoteIceCandidate = remoteIceCandidate
        )
    }

    companion object {
        // TODO same url for production?
//        private const val BASE_URL = "wss://signaling-server-dev.rdx-works-main.extratools.works/"
        private const val BASE_URL = "wss://signaling-server-betanet.radixdlt.com/"
    }
}
