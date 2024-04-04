package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WellKnownDAppDefinitionResponse(
    @SerialName(value = "dApps")
    val dApps: List<WellKnownDAppDefinition>,
    @SerialName(value = "callbackPath")
    val callbackPath: String? = null
) {

    @Serializable
    data class WellKnownDAppDefinition(
        @SerialName(value = "dAppDefinitionAddress")
        val dAppDefinitionAddress: String,
    )
}
