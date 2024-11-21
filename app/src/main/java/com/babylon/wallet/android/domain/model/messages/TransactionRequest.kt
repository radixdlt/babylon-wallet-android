package com.babylon.wallet.android.domain.model.messages

import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.TransactionToReview
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.Curve25519SecretKey

data class TransactionRequest(
    override val remoteEntityId: RemoteEntityID,
    override val interactionId: WalletInteractionId,
    private val unvalidatedManifestData: UnvalidatedManifestData,
    val requestMetadata: RequestMetadata,
    val kind: Kind
) : DappToWalletInteraction(remoteEntityId, interactionId, requestMetadata) {

    val instructions = unvalidatedManifestData.instructions
    val message = unvalidatedManifestData.message
    val networkId = unvalidatedManifestData.networkId

    sealed interface Kind {

        data class Regular(
            val transactionType: TransactionType,
            val transactionToReview: TransactionToReview,
            val ephemeralNotaryPrivateKey: Curve25519SecretKey
        ) : Kind

        data class PreAuthorized(
            val expiration: SubintentExpiration,
            val preAuthToReview: PreAuthToReview
        ) : Kind

        val isPreAuthorized: Boolean
            get() = this is PreAuthorized
    }
}
