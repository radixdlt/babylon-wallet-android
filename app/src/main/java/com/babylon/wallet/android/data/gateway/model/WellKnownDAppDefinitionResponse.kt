package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WellKnownDAppDefinitionResponse(
    @SerialName(value = "dApps")
    val dApps: List<WellKnownDAppDefinitionAddress>
) {

    @Serializable
    data class WellKnownDAppDefinitionAddress(
        @SerialName(value = "dAppDefinitionAddress")
        val dAppDefinitionAddress: String
    )
}
