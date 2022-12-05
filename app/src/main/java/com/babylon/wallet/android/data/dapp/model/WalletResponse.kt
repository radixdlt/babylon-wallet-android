package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WalletResponse(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("items")
    val items: List<WalletResponseItem>
)

@Serializable
sealed class WalletResponseItem {
    abstract val requestType: String
}
