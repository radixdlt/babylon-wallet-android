package com.babylon.wallet.android.data.ce.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendTransactionResponseItem(
    @SerialName("transactionIntentHash")
    val transactionIntentHash: String,
)
