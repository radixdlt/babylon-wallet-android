package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("method")
sealed class RcrHandshakeRequest {
    @Serializable
    @SerialName("sendHandshakeRequest")
    data class SendHandshake(
        @SerialName("sessionId")
        val sessionId: String,
        @SerialName("data")
        val publicKeyHex: String
    ) : RcrHandshakeRequest()

    @Serializable
    @SerialName("getHandshakeRequest")
    data class GetHandshake(
        @SerialName("sessionId")
        val sessionId: String
    ) : RcrHandshakeRequest()

    @Serializable
    @SerialName("sendHandshakeResponse")
    data class SendHandshakeResponse(
        @SerialName("sessionId")
        val sessionId: String,
        @SerialName("data")
        val publicKeyHex: String
    ) : RcrHandshakeRequest()

    @Serializable
    @SerialName("getHandshakeResponse")
    data class GetHandshakeResponse(
        @SerialName("sessionId")
        val sessionId: String
    ) : RcrHandshakeRequest()
}
