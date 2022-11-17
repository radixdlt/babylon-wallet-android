package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GatewayErrorResponse(
    @SerialName(value = "errors")
    val errors: Map<String, List<String>> = emptyMap(),
    @SerialName(value = "type")
    val type: String? = null,
    @SerialName(value = "title")
    val title: String? = null,
    @SerialName(value = "status")
    val status: Int,
    @SerialName(value = "traceId")
    val traceId: Int,
)
