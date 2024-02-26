@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.radixdlt.ret.TransactionHeader
import com.radixdlt.ret.TransactionManifest
import java.util.UUID

fun TransactionManifest.prepareInternalTransactionRequest(
    networkId: Int,
    requestId: String = UUID.randomUUID().toString(),
    message: String? = null,
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteConnectorId = "",
    requestId = requestId,
    transactionManifestData = TransactionManifestData.from(
        manifest = this,
        networkId = networkId,
        message = message
    ),
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
)

fun TransactionHeader.toPrettyString(): String = StringBuilder()
    .appendLine("[Start Epoch]         => $startEpochInclusive")
    .appendLine("[End Epoch]           => $endEpochExclusive")
    .appendLine("[Network id]          => $networkId")
    .appendLine("[Nonce]               => $nonce")
    .appendLine("[Notary is signatory] => $notaryIsSignatory")
    .appendLine("[Tip %]               => $tipPercentage")
    .toString()

fun TransactionManifest.toPrettyString(): String {
    val blobSeparator = "\n"
    val blobPreamble = "BLOBS\n"
    val blobLabel = "BLOB\n"

    val instructionsFormatted = instructions().asStr()

    val blobsByByteCount = blobs().mapIndexed { index, bytes ->
        "$blobLabel[$index]: #${bytes.size} bytes"
    }.joinToString(blobSeparator)

    val blobsString = if (blobsByByteCount.isNotEmpty()) {
        listOf(blobPreamble, blobsByByteCount).joinToString(separator = blobSeparator)
    } else {
        ""
    }

    return "$instructionsFormatted$blobsString"
}
