package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.WalletInteractionId

data class TransactionRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val unvalidatedManifestData: UnvalidatedManifestData,
    val requestMetadata: RequestMetadata,
    val transactionType: TransactionType = TransactionType.Generic
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata)
