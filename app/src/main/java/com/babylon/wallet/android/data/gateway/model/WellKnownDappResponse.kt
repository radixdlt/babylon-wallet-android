package com.babylon.wallet.android.data.gateway.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WellKnownDappResponse(
    @SerialName(value = "dApps")
    val dapps: List<DappMetadataDto> = emptyList()
)
