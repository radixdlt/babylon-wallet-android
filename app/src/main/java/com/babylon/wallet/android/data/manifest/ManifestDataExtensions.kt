@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.radixdlt.sargon.WalletInteractionId
import rdx.works.core.domain.TransactionManifestData

fun TransactionManifestData.prepareInternalTransactionRequest(
    requestId: WalletInteractionId = WalletInteractionId.randomUUID(),
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = IncomingMessage.IncomingRequest.TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteEntityId = IncomingMessage.RemoteEntityID.ConnectorId(""),
    interactionId = requestId,
    transactionManifestData = this,
    requestMetadata = IncomingMessage.IncomingRequest.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
)
