package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    @SerialName("address") val address: String,
    @SerialName("label") val label: String,
    @SerialName("appearanceId") val appearanceId: Int
)
