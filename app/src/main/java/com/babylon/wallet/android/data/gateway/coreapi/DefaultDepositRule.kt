package com.babylon.wallet.android.data.gateway.coreapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DefaultDepositRule {
    @SerialName("Accept")
    Accept,

    @SerialName("Reject")
    Reject,

    @SerialName("AllowExisting")
    AllowExisting
}