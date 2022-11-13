package rdx.works.peerdroid.data.websocket.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SignalingServerResponse(
    @SerialName("info")
    val info: String, // describes the type of response, "confirmation", "error", ...
    @SerialName("requestId")
    val requestId: String? = null,
    @SerialName("data")
    val data: RpcMessage? = null,
    @SerialName("error")
    val error: String? = null,
) {

    enum class Info(val value: String) {
        CONFIRMATION("confirmation"),

        DATA_FROM_BROWSER_EXTENSION("remoteData"),

        REMOTE_CLIENT_DISCONNECTED("remoteClientDisconnected"),
        REMOTE_CLIENT_IS_ALREADY_CONNECTED("remoteClientIsAlreadyConnected"),
        REMOTE_CLIENT_JUST_CONNECTED("remoteClientJustConnected"),

        MISSING_REMOTE_CLIENT_ERROR("missingRemoteClientError"),
        INVALID_MESSAGE_ERROR("invalidMessageError"),
        VALIDATION_ERROR("validationError");

        companion object {
            fun from(value: String): Info = requireNotNull(
                values().find { info -> info.value == value }
            ) {
                "No Info with value $value"
            }
        }
    }
}
