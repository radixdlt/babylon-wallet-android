package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("method")
sealed class RcrRequest {
    @Serializable
    @SerialName("sendRequest")
    data class SendRequest(
        @SerialName("sessionId")
        val sessionId: String,
        @SerialName("data")
        val data: String,
    ) : RcrRequest()

    @Serializable
    @SerialName("getRequests")
    data class GetRequests(
        @SerialName("sessionId")
        val sessionId: String,
    ) : RcrRequest()

    @Serializable
    @SerialName("sendResponse")
    data class SendResponse(
        @SerialName("sessionId")
        val sessionId: String,
        @SerialName("data")
        val data: String,
    ) : RcrRequest()

    @Serializable
    @SerialName("getResponses")
    data class GetResponses(
        @SerialName("sessionId")
        val sessionId: String,
    ) : RcrRequest()
}
