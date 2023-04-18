package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendTransactionResponseItem(
    @SerialName("transactionIntentHash")
    val transactionIntentHash: String,
)
