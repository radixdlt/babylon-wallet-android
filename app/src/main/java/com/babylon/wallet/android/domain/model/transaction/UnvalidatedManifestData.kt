package com.babylon.wallet.android.domain.model.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.radixdlt.sargon.BagOfBytes
import com.radixdlt.sargon.Message
import com.radixdlt.sargon.MessageV2
import com.radixdlt.sargon.NetworkId
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.instructionsString
import com.radixdlt.sargon.extensions.plaintext
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.newMessageV2PlaintextString
import java.util.UUID

data class UnvalidatedManifestData(
    val instructions: String,
    val networkId: NetworkId,
    val plainMessage: String?,
    val blobs: List<BagOfBytes> = emptyList(),
) {

    val message: Message by lazy {
        plainMessage?.let { Message.plaintext(it) } ?: Message.None
    }

    val messageV2: MessageV2 by lazy {
        plainMessage?.let { newMessageV2PlaintextString(it) } ?: MessageV2.None
    }

    companion object {
        fun from(
            manifest: TransactionManifest,
            message: String? = null
        ) = UnvalidatedManifestData(
            instructions = manifest.instructionsString,
            plainMessage = message,
            networkId = manifest.networkId,
            blobs = manifest.blobs.toList().map { it.bytes },
        )
    }
}

fun UnvalidatedManifestData.prepareInternalTransactionRequest(
    requestId: WalletInteractionId = UUID.randomUUID().toString(),
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteEntityId = RemoteEntityID.ConnectorId(""),
    interactionId = requestId,
    unvalidatedManifestData = this,
    requestMetadata = DappToWalletInteraction.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
)
