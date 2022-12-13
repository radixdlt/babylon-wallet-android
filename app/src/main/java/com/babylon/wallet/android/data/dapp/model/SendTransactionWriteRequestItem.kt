package com.babylon.wallet.android.data.dapp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendTransactionWriteRequestItem(
    override val requestType: String,
    @SerialName("transactionManifest")
    val transactionManifest: String,
    @SerialName("version")
    val version: Long,
    @SerialName("blobs")
    val blobs: List<String>? = null,
    @SerialName("message")
    val message: String? = null,
) : WalletResponseItem()