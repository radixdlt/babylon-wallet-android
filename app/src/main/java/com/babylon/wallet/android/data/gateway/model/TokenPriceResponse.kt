package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenPriceResponse(
    val id: String,
    @SerialName(value = "resource_address")
    val resourceAddress: String,
    val symbol: String,
    val name: String,
    val price: Double,
    val currency: String
)
