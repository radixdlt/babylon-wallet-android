package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.Serializable

@Serializable
data class TokensAndLsusPricesRequest(
    val currency: String,
    val lsus: List<String>,
    val tokens: List<String>
)
