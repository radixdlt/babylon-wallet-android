package rdx.works.peerdroid.data.websocket

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeHex
import rdx.works.core.decrypt
import rdx.works.core.encrypt
import rdx.works.core.toHexString
import rdx.works.peerdroid.BuildConfig
import rdx.works.peerdroid.data.webrtc.model.PeerConnectionEvent
import rdx.works.peerdroid.data.webrtc.model.RemoteIceCandidate
import rdx.works.peerdroid.data.websocket.model.RpcMessage
import rdx.works.peerdroid.data.websocket.model.RpcMessage.IceCandidatePayload.Companion.toJsonPayload
import rdx.works.peerdroid.data.websocket.model.SignalingServerDto
import rdx.works.peerdroid.data.websocket.model.SignalingServerMessage
import timber.log.Timber
import java.nio.charset.StandardCharsets

internal class WebSocketClient(applicationContext: Context) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HttpClientEntryPoint {
        fun provideHttpClient(): HttpClient
    }

    private val httpClientEntryPoint: HttpClientEntryPoint = EntryPointAccessors.fromApplication(
        context = applicationContext,
        entryPoint = HttpClientEntryPoint::class.java
    )

    private val json: Json = Json {
        ignoreUnknownKeys = true
    }

    // represents a web socket session between two peers
    private var socket: WebSocketSession? = null

    private lateinit var connectionId: String
    private lateinit var encryptionKey: ByteArray

    // this is used only for the PeerdroidLink
    // where we need to add a new link connection
    // therefore no remote client (dapp) is involved
    private var connectorExtensionId = ""

    init {
        Timber.d("🛰 initialize WebSocketClient")
    }

    suspend fun initSession(
        connectionId: String,
        encryptionKey: ByteArray
    ): Result<Unit> {
        return try {
            this.connectionId = connectionId
            this.encryptionKey = encryptionKey

            // this block has a normal http request builder
            // because we need to do an http request once (initial handshake)
            // to establish the connection the first time
            socket = httpClientEntryPoint.provideHttpClient().webSocketSession {
                url("${BuildConfig.SIGNALING_SERVER_URL}$connectionId?source=wallet&target=extension")
            }
            if (socket?.isActive == true) {
                Timber.d("🛰 successfully connected to signaling server")
                Result.success(Unit)
            } else {
                Timber.e("🛰 failed to connect to signaling server❗")
                Result.failure(Exception("Couldn't establish a connection"))
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("🛰 connection exception: ${exception.localizedMessage}❗")
            Result.failure(exception)
        }
    }

    fun listenForMessages(): Flow<SignalingServerMessage> {
        return try {
            socket?.incoming // web socket channel
                ?.receiveAsFlow()
                ?.cancellable()
                ?.filterIsInstance<Frame.Text>()
                ?.mapNotNull { frameText ->
                    val messageJsonString = frameText.readText()
                    decodeAndParseMessageFromJson(messageJsonString)
                }
                ?.map { signalingServerMessage ->
                    if (signalingServerMessage is SignalingServerMessage.RemoteInfo.ClientConnected) {
                        connectorExtensionId = signalingServerMessage.remoteClientId
                    }
                    signalingServerMessage
                }
                ?: flowOf(SignalingServerMessage.Error.Unknown)
        } catch (exception: Exception) {
            Timber.e("🛰 incoming message exception: ${exception.localizedMessage}❗")
            flowOf(SignalingServerMessage.Error.Unknown)
        }
    }

    // not used at the moment, but in the future when the wallet will send the offer
    suspend fun sendOfferMessage(
        remoteClientId: String,
        offerPayload: RpcMessage.OfferPayload
    ) {
        val offerJson = json.encodeToString(offerPayload)
        val encryptedOffer = offerJson.toByteArray().encrypt(
            withEncryptionKey = encryptionKey
        )
        val rpcMessage = RpcMessage.Offer(
            targetClientId = remoteClientId.ifEmpty { connectorExtensionId },
            encryptedPayload = encryptedOffer.getOrThrow().toHexString()
        )
        val message = json.encodeToString(rpcMessage)
        Timber.d("🛰 sending offer to remoteClient: $remoteClientId with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    suspend fun sendAnswerMessage(
        remoteClientId: String,
        answerPayload: RpcMessage.AnswerPayload
    ) {
        val answerJson = json.encodeToString(answerPayload)
        val encryptedAnswer = answerJson.toByteArray().encrypt(
            withEncryptionKey = encryptionKey
        )
        val rpcMessage: RpcMessage = RpcMessage.Answer(
            targetClientId = remoteClientId.ifEmpty { connectorExtensionId },
            encryptedPayload = encryptedAnswer.getOrThrow().toHexString()
        )
        val message = json.encodeToString(rpcMessage)
        Timber.d("🛰 sending answer to remoteClient: $remoteClientId with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    suspend fun sendIceCandidateMessage(
        remoteClientId: String,
        iceCandidateData: PeerConnectionEvent.IceCandidate.Data
    ) {
        val iceCandidatePayload = iceCandidateData.toJsonPayload()
        val encryptedIceCandidate = iceCandidatePayload.toString().toByteArray().encrypt(
            withEncryptionKey = encryptionKey
        )
        val rpcMessage: RpcMessage = RpcMessage.IceCandidate(
            targetClientId = remoteClientId.ifEmpty { connectorExtensionId },
            encryptedPayload = encryptedIceCandidate.getOrThrow().toHexString()
        )
        val message = json.encodeToString(rpcMessage)
        Timber.d("🛰 sending ice candidate to remoteClient: $remoteClientId with requestId: ${rpcMessage.requestId}")
        sendMessage(message)
    }

    suspend fun closeSession() {
        socket?.close()
    }

    private suspend fun sendMessage(message: String) {
        try {
            socket?.send(Frame.Text(message))
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                throw exception
            }
            Timber.e("🛰 failed to send message: ${exception.localizedMessage}❗")
        }
    }

    private fun decodeAndParseMessageFromJson(messageJsonString: String): SignalingServerMessage {
        return when (val signalingServerDto = json.decodeFromString<SignalingServerDto>(messageJsonString)) {
            is SignalingServerDto.RemoteClientDisconnected -> {
                SignalingServerMessage.RemoteInfo.ClientDisconnected(remoteClientId = signalingServerDto.remoteClientId)
            }
            is SignalingServerDto.RemoteClientIsAlreadyConnected -> {
                SignalingServerMessage.RemoteInfo.ClientConnected(remoteClientId = signalingServerDto.remoteClientId)
            }
            is SignalingServerDto.RemoteClientJustConnected -> {
                SignalingServerMessage.RemoteInfo.ClientConnected(remoteClientId = signalingServerDto.remoteClientId)
            }
            is SignalingServerDto.MissingRemoteClientError -> {
                SignalingServerMessage.RemoteInfo.MissingClient(requestId = signalingServerDto.requestId)
            }
            is SignalingServerDto.Confirmation -> {
                SignalingServerMessage.Confirmation(requestId = signalingServerDto.requestId)
            }
            is SignalingServerDto.RemoteData -> {
                return when (signalingServerDto.data) {
                    is RpcMessage.Offer -> {
                        decryptAndParseOfferPayload(signalingServerDto.data, signalingServerDto.remoteClientId)
                    }
                    is RpcMessage.Answer -> {
                        decryptAndParseAnswerPayload(signalingServerDto.data, signalingServerDto.remoteClientId)
                    }
                    is RpcMessage.IceCandidate -> {
                        decryptAndParseIceCandidatePayload(signalingServerDto.data, signalingServerDto.remoteClientId)
                    }
                }
            }
            is SignalingServerDto.InvalidMessageError -> {
                SignalingServerMessage.Error.InvalidMessage(
                    errorMessage = signalingServerDto.error
                )
            }
            is SignalingServerDto.ValidationError -> {
                Timber.e("🛰 validation error in signaling server message: ${signalingServerDto.error}❗")
                SignalingServerMessage.Error.Validation
            }
        }
    }

    private fun decryptAndParseOfferPayload(
        offerPayload: RpcMessage.Offer,
        remoteClientId: String
    ): SignalingServerMessage.RemoteData.Offer {
        val message = offerPayload.encryptedPayload.decodeHex().toByteArray().decrypt(
            withEncryptionKey = encryptionKey
        ).getOrThrow()
        val offer = json.decodeFromString<RpcMessage.OfferPayload>(String(message, StandardCharsets.UTF_8))

        return SignalingServerMessage.RemoteData.Offer(
            remoteClientId = remoteClientId,
            targetClientId = offerPayload.targetClientId,
            requestId = offerPayload.requestId,
            sdp = offer.sdp
        )
    }

    // not used at the moment, but in the future when the wallet will send the offer
    private fun decryptAndParseAnswerPayload(
        answerPayload: RpcMessage.Answer,
        remoteClientId: String
    ): SignalingServerMessage.RemoteData.Answer {
        val message = answerPayload.encryptedPayload.decodeHex().toByteArray().decrypt(
            withEncryptionKey = encryptionKey
        ).getOrThrow()
        val answer = json.decodeFromString<RpcMessage.AnswerPayload>(String(message, StandardCharsets.UTF_8))

        return SignalingServerMessage.RemoteData.Answer(
            remoteClientId = remoteClientId,
            targetClientId = answerPayload.targetClientId,
            requestId = answerPayload.requestId,
            sdp = answer.sdp
        )
    }

    private fun decryptAndParseIceCandidatePayload(
        iceCandidatePayload: RpcMessage.IceCandidate,
        remoteClientId: String
    ): SignalingServerMessage.RemoteData.IceCandidate {
        val message = iceCandidatePayload.encryptedPayload.decodeHex().toByteArray().decrypt(
            withEncryptionKey = encryptionKey
        ).getOrThrow()
        val iceCandidate = json.decodeFromString<RpcMessage.IceCandidatePayload>(
            String(message, StandardCharsets.UTF_8)
        )

        val remoteIceCandidate = RemoteIceCandidate(
            sdpMid = iceCandidate.sdpMid,
            sdpMLineIndex = iceCandidate.sdpMLineIndex,
            candidate = iceCandidate.candidate
        )

        return SignalingServerMessage.RemoteData.IceCandidate(
            remoteClientId = remoteClientId,
            targetClientId = iceCandidatePayload.targetClientId,
            requestId = iceCandidatePayload.requestId,
            remoteIceCandidate = remoteIceCandidate
        )
    }
}
