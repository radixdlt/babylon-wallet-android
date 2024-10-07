package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.radixdlt.sargon.WalletInteractionId
import rdx.works.core.domain.TransactionManifestData

data class TransactionRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val transactionManifestData: TransactionManifestData,
    val requestMetadata: RequestMetadata,
    val transactionType: TransactionType = TransactionType.Generic
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata)
