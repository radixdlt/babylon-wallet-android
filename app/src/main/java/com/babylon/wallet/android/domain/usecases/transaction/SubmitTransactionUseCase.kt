package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import javax.inject.Inject

class SubmitTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val cache: HttpCache
) {

    suspend operator fun invoke(
        txIDHash: String,
        notarizedTransactionHex: String,
        txProcessingTime: String
    ): Result<SubmitTransactionResult> {
        val submitResult = transactionRepository.submitTransaction(
            notarizedTransaction = notarizedTransactionHex
        )
        return submitResult.getOrNull()?.let { result ->
            // Invalidate all cached information stored, since a transaction may mutate
            // some resource information
            cache.invalidate()

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
                        txProcessingTime = txProcessingTime
                    )
                )
            }
        } ?: Result.failure(
            RadixWalletException.PrepareTransactionException.SubmitNotarizedTransaction(submitResult.exceptionOrNull())
        )
    }

    data class SubmitTransactionResult(
        val txId: String,
        val txProcessingTime: String
    )
}
