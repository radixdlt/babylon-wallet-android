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
data class WalletErrorResponse(
    @SerialName("requestId")
    val requestId: String,
    @SerialName("error")
    val error: WalletErrorType,
    @SerialName("message")
    val message: String? = null
)

@Serializable
sealed class WalletResponseItem {
    abstract val requestType: String
}
