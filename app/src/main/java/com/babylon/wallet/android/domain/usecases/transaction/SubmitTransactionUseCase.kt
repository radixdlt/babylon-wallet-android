package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import javax.inject.Inject

class SubmitTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        txIDHash: String,
        notarizedTransactionHex: String,
        endEpoch: ULong
    ): Result<SubmitTransactionResult> {
        val submitResult = transactionRepository.submitTransaction(
            notarizedTransaction = notarizedTransactionHex
        )
        return submitResult.getOrNull()?.let { result ->
            if (result.duplicate) {
                Result.failure(
                    RadixWalletException.TransactionSubmitException.InvalidTXDuplicate(
                        txIDHash
                    )
                )
            } else {
                Result.success(
                    SubmitTransactionResult(
                        txId = txIDHash,
                        endEpoch = endEpoch
                    )
                )
            }
        } ?: Result.failure(
            RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction(submitResult.exceptionOrNull())
        )
    }

    data class SubmitTransactionResult(
        val txId: String,
        val endEpoch: ULong
    )
}
