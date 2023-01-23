package com.babylon.wallet.android.data.dapp.model

import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.hex.decode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendTransactionItem(
    @SerialName("transactionManifest")
    val transactionManifest: String,
    @SerialName("version")
    val version: Long,
    @SerialName("blobs")
    val blobs: List<String>? = null,
    @SerialName("message")
    val message: String? = null
) : WalletInteractionItems()

fun SendTransactionItem.toDomainModel(requestId: String, networkId: Int) =
    MessageFromDataChannel.IncomingRequest.TransactionItem(
        requestId = requestId,
        networkId = networkId,
        transactionManifestData = TransactionManifestData(
            transactionManifest,
            version,
            networkId,
            blobs?.map { decode(it) }.orEmpty()
        )
    )
