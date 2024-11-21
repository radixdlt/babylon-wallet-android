package com.babylon.wallet.android.domain.usecases.interaction

import com.babylon.wallet.android.data.dapp.InteractionPreviewProvider
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.model.messages.DappToWalletInteraction
import com.babylon.wallet.android.domain.model.messages.RemoteEntityID
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.WalletInteractionId
import com.radixdlt.sargon.extensions.init
import java.util.UUID
import javax.inject.Inject

class PrepareInternalTransactionUseCase @Inject constructor(
    private val interactionPreviewProvider: InteractionPreviewProvider
) {

    suspend operator fun invoke(
        unvalidatedManifestData: UnvalidatedManifestData,
        requestId: WalletInteractionId = UUID.randomUUID().toString(),
        blockUntilCompleted: Boolean = false,
        transactionType: TransactionType = TransactionType.Generic
    ): Result<TransactionRequest> = runCatching {
        val transactionToReviewOutcome = interactionPreviewProvider.analyseTransactionPreview(
            instructions = unvalidatedManifestData.instructions,
            blobs = Blobs.init(unvalidatedManifestData.blobs.map { Blob.init(it) }),
            isInternal = true
        ).getOrThrow()

        TransactionRequest(
            // Since we mock this request as a dApp request from the wallet app, the dApp's id is empty.
            // Should never be invoked as we always check if a request is not internal before sending message to the dApp
            remoteEntityId = RemoteEntityID.ConnectorId(""),
            interactionId = requestId,
            unvalidatedManifestData = unvalidatedManifestData,
            requestMetadata = DappToWalletInteraction.RequestMetadata.internal(
                networkId = unvalidatedManifestData.networkId,
                blockUntilCompleted = blockUntilCompleted
            ),
            kind = TransactionRequest.Kind.Regular(
                transactionType = transactionType,
                transactionToReview = transactionToReviewOutcome.transactionToReview,
                ephemeralNotaryPrivateKey = transactionToReviewOutcome.ephemeralNotaryPrivateKey
            )
        )
    }
}
