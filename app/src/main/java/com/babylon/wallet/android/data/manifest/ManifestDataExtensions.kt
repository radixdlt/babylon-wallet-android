@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.data.manifest

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import rdx.works.profile.ret.transaction.TransactionManifestData
import java.util.UUID

fun TransactionManifestData.prepareInternalTransactionRequest(
    requestId: String = UUID.randomUUID().toString(),
    blockUntilCompleted: Boolean = false,
    transactionType: TransactionType = TransactionType.Generic
) = MessageFromDataChannel.IncomingRequest.TransactionRequest(
    // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty. Should never be invoked as we always
    // check if a request is not internal before sending message to the dApp
    remoteConnectorId = "",
    requestId = requestId,
    transactionManifestData = this,
    requestMetadata = MessageFromDataChannel.IncomingRequest.RequestMetadata.internal(networkId, blockUntilCompleted),
    transactionType = transactionType
)