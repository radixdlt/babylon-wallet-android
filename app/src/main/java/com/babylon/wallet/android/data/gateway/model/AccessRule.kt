package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessRule(
    @SerialName("type") val type: Type
) {
    @Serializable
    enum class Type {
        @SerialName("DenyAll")
        DenyAll,

        @SerialName("AllowAll")
        AllowAll,

        @SerialName("Protected")
        Protected
    }
}
