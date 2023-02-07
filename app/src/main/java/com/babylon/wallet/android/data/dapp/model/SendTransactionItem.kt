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
)

fun SendTransactionItem.toDomainModel(
    requestId: String,
    metadata: MessageFromDataChannel.IncomingRequest.RequestMetadata
) =
    MessageFromDataChannel.IncomingRequest.TransactionRequest(
        requestId = requestId,
        transactionManifestData = TransactionManifestData(
            transactionManifest,
            version,
            metadata.networkId,
            blobs?.map { decode(it) }.orEmpty()
        ),
        metadata
    )
