package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendTransactionResponseItem(
    override val requestType: String,
    @SerialName("transactionIntentHash")
    val transactionIntentHash: String
) : WalletResponseItem() {
    companion object {
        const val REQUEST_TYPE = "sendTransactionWrite"
    }
}
