package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.CompiledNotarizedIntent
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.SignedIntentHash
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hex
import rdx.works.core.mapError
import rdx.works.core.then
import javax.inject.Inject

class SubmitTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        signedIntentHash: SignedIntentHash,
        compiledNotarizedIntent: CompiledNotarizedIntent,
        endEpoch: Epoch
    ): Result<SubmitTransactionResult> = transactionRepository.submitTransaction(
        notarizedTransaction = compiledNotarizedIntent.bytes.hex
    ).mapError { error ->
        RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction(error)
    }.then { result ->
        if (result.duplicate) {
            Result.failure(RadixWalletException.TransactionSubmitException.InvalidTXDuplicate(signedIntentHash.bech32EncodedTxId))
        } else {
            Result.success(
                SubmitTransactionResult(
                    txId = signedIntentHash.bech32EncodedTxId,
                    endEpoch = endEpoch
                )
            )
        }
    }

    data class SubmitTransactionResult(
        val txId: String,
        val endEpoch: Epoch
    )
}
