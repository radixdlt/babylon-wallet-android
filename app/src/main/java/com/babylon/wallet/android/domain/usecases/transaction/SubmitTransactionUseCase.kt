package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.TransactionSubmitException
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.hex
import rdx.works.core.domain.transaction.NotarizationResult
import rdx.works.core.mapError
import rdx.works.core.then
import javax.inject.Inject

class SubmitTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        notarizationResult: NotarizationResult
    ): Result<NotarizationResult> = transactionRepository.submitTransaction(
        notarizedTransaction = notarizationResult.compiledNotarizedIntent.bytes.hex
    ).mapError { error ->
        RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction(error)
    }.then { result ->
        if (result.duplicate) {
            Result.failure(TransactionSubmitException.InvalidTXDuplicate(notarizationResult.intentHash.bech32EncodedTxId))
        } else {
            Result.success(notarizationResult)
        }
    }
}
