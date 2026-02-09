package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.DappToWalletInteractionSubintentHeader
import com.radixdlt.sargon.WalletInteractionId

data class TransactionRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    val unvalidatedManifestData: UnvalidatedManifestData,
    val requestMetadata: RequestMetadata,
    val kind: Kind
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata) {

    sealed interface Kind {

        data class Regular(
            val transactionType: TransactionType
        ) : Kind

        data class PreAuthorized(
            val expiration: SubintentExpiration,
            val header: DappToWalletInteractionSubintentHeader?
        ) : Kind

        val isPreAuthorized: Boolean
            get() = this is PreAuthorized
    }
}
