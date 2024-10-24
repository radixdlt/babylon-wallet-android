package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.radixdlt.sargon.WalletInteractionId
import rdx.works.core.domain.UnvalidatedManifestData

data class TransactionRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val unvalidatedManifestData: UnvalidatedManifestData,
    val requestMetadata: RequestMetadata,
    val transactionType: TransactionType = TransactionType.Generic
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata)
