package com.babylon.wallet.android.data.profile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    @SerialName("address")
    val address: String
)
