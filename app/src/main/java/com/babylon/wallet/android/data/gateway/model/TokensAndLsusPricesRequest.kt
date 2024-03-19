package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokensAndLsusPricesRequest(
    @SerialName("currency")
    val currency: String,
    @SerialName("lsus")
    val lsus: List<String>,
    @SerialName("tokens")
    val tokens: List<String>
)
