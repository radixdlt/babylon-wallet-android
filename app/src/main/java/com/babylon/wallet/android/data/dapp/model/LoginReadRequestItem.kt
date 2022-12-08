package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("loginRead")
data class LoginReadRequestItem(
    @SerialName("challenge")
    val challenge: String?
) : WalletRequestItem()
