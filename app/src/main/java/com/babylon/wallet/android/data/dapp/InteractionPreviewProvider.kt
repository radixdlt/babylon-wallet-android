package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.TransactionToReviewOutcome
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.random
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InteractionPreviewProvider @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend fun analyseTransactionPreview(
        instructions: String,
        blobs: Blobs,
        isInternal: Boolean
    ): TransactionToReviewOutcome {
        return withContext(dispatcher) {
            val ephemeralNotaryPrivateKey = Curve25519SecretKey.secureRandom()
            val transactionToReview = sargonOsManager.sargonOs.analyseTransactionPreview(
                instructions = instructions,
                blobs = blobs,
                areInstructionsOriginatingFromHost = isInternal,
                nonce = Nonce.random(),
                notaryPublicKey = ephemeralNotaryPrivateKey.toPublicKey()
            )

            TransactionToReviewOutcome(
                transactionToReview = transactionToReview,
                ephemeralNotaryPrivateKey = ephemeralNotaryPrivateKey
            )
        }
    }

    suspend fun analysePreAuthPreview(
        instructions: String,
        blobs: Blobs
    ): PreAuthToReview {
        return withContext(dispatcher) {
            sargonOsManager.sargonOs.analysePreAuthPreview(
                instructions = instructions,
                blobs = blobs,
                nonce = Nonce.random(),
            )
        }
    }
}