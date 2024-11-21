package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.toDomainModel
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.radixdlt.sargon.DappToWalletInteractionItems
import com.radixdlt.sargon.DappToWalletInteractionUnvalidated
import javax.inject.Inject

class DAppToWalletInteractionProcessor @Inject constructor(
    private val interactionPreviewProvider: InteractionPreviewProvider
) {

    suspend fun process(
        remoteEntityId: RemoteEntityID,
        unvalidatedInteraction: DappToWalletInteractionUnvalidated
    ): Result<DappToWalletInteraction> = runCatching {
        val metadata = DappToWalletInteraction.RequestMetadata(
            networkId = unvalidatedInteraction.metadata.networkId,
            origin = unvalidatedInteraction.metadata.origin,
            dAppDefinitionAddress = unvalidatedInteraction.metadata.dappDefinitionAddress,
            isInternal = false
        )
        when (val items = unvalidatedInteraction.items) {
            is DappToWalletInteractionItems.AuthorizedRequest -> items.v1.toDomainModel(
                remoteEntityId = remoteEntityId,
                interactionId = unvalidatedInteraction.interactionId,
                metadata = metadata
            )

            is DappToWalletInteractionItems.UnauthorizedRequest -> items.v1.toDomainModel(
                remoteEntityId = remoteEntityId,
                requestId = unvalidatedInteraction.interactionId,
                metadata = metadata
            )

            is DappToWalletInteractionItems.Transaction -> {
                val unvalidatedManifest = items.v1.send.unvalidatedManifest
                val transactionToReviewOutcome = interactionPreviewProvider.analyseTransactionPreview(
                    instructions = unvalidatedManifest.transactionManifestString,
                    blobs = unvalidatedManifest.blobs,
                    isInternal = false
                ).getOrThrow()
                items.v1.send.toDomainModel(
                    remoteConnectorId = remoteEntityId,
                    requestId = unvalidatedInteraction.interactionId,
                    metadata = metadata,
                    transactionToReviewOutcome = transactionToReviewOutcome
                )
            }

            is DappToWalletInteractionItems.PreAuthorization -> {
                val unvalidatedManifest = items.v1.request.unvalidatedManifest
                val preAuthToReview = interactionPreviewProvider.analysePreAuthPreview(
                    instructions = unvalidatedManifest.subintentManifestString,
                    blobs = unvalidatedManifest.blobs
                ).getOrThrow()
                items.v1.request.toDomainModel(
                    remoteConnectorId = remoteEntityId,
                    requestId = unvalidatedInteraction.interactionId,
                    metadata = metadata,
                    preAuthToReview = preAuthToReview
                )
            }
        }
    }
}
