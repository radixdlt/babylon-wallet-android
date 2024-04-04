package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RcrResponse(
    @SerialName("data")
    val data: List<String>? = null
)