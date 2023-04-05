package rdx.works.peerdroid.data.websocket.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("info")
internal sealed class SignalingServerDto {

    @Serializable
    @SerialName("confirmation")
    data class Confirmation(
        @SerialName("requestId")
        val requestId: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("remoteData")
    data class RemoteData(
        @SerialName("requestId")
        val requestId: String,
        @SerialName("remoteClientId")
        val remoteClientId: String,
        @SerialName("data")
        val data: RpcMessage
    ) : SignalingServerDto()

    @Serializable
    @SerialName("remoteClientDisconnected")
    data class RemoteClientDisconnected(
        @SerialName("remoteClientId")
        val remoteClientId: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("remoteClientIsAlreadyConnected")
    data class RemoteClientIsAlreadyConnected(
        @SerialName("remoteClientId")
        val remoteClientId: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("remoteClientJustConnected")
    data class RemoteClientJustConnected(
        @SerialName("remoteClientId")
        val remoteClientId: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("missingRemoteClientError")
    data class MissingRemoteClientError(
        @SerialName("requestId")
        val requestId: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("invalidMessageError")
    data class InvalidMessageError(
        @SerialName("error")
        val error: String,
        @SerialName("data")
        val data: String
    ) : SignalingServerDto()

    @Serializable
    @SerialName("validationError")
    data class ValidationError(
        @SerialName("requestId")
        val requestId: String,
        @SerialName("error")
        val error: List<String>
    ) : SignalingServerDto()
}
