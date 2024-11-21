package com.babylon.wallet.android.data.dapp

import com.babylon.wallet.android.data.dapp.model.TransactionToReviewOutcome
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.extensions.Curve25519SecretKey
import com.radixdlt.sargon.extensions.mapError
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
    ): Result<TransactionToReviewOutcome> {
        return withContext(dispatcher) {
            runCatching {
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
            }.mapError(::mapError)
        }
    }

    suspend fun analysePreAuthPreview(
        instructions: String,
        blobs: Blobs
    ): Result<PreAuthToReview> {
        return withContext(dispatcher) {
            runCatching {
                sargonOsManager.sargonOs.analysePreAuthPreview(
                    instructions = instructions,
                    blobs = blobs,
                    nonce = Nonce.random(),
                )
            }.mapError(::mapError)
        }
    }

    private fun mapError(throwable: Throwable): RadixWalletException {
        return when (throwable) {
            is CommonException.ReservedInstructionsNotAllowedInManifest -> {
                RadixWalletException.DappRequestException.UnacceptableManifest
            }
            is CommonException.OneOfReceivingAccountsDoesNotAllowDeposits -> {
                RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits
            }
            else -> {
                RadixWalletException.DappRequestException.PreviewError(throwable)
            }
        }
    }
}
