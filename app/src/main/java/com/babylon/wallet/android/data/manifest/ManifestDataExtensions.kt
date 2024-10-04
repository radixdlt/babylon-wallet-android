package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.messages.IncomingMessage.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.radixdlt.sargon.WalletInteractionId
import rdx.works.core.domain.TransactionManifestData
import java.util.UUID

fun TransactionManifestData.prepareInternalTransactionRequest(
    requestId: WalletInteractionId = UUID.randomUUID().toString(),
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteEntityId = RemoteEntityID.ConnectorId(""),
    interactionId = requestId,
    transactionManifestData = this,
    requestMetadata = DappToWalletInteraction.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
)
