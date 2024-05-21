package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RcrHandshakeResponse(
    @SerialName("publicKey")
    val publicKeyHex: String
)
