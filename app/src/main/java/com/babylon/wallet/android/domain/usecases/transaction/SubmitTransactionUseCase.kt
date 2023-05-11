package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import javax.inject.Inject
import rdx.works.core.toHexString as toHex

class SubmitTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val cache: HttpCache
) {

    suspend operator fun invoke(
        txID: String,
        compiledNotarizedIntent: ByteArray
    ): Result<String> {
        return submitNotarizedTransaction(txID, compiledNotarizedIntent)
    }

    private suspend fun submitNotarizedTransaction(
        txID: String,
        notarizedTransaction: ByteArray,
    ): Result<String> {
        val submitResult = transactionRepository.submitTransaction(
            notarizedTransaction = notarizedTransaction.toHex()
        )
        return when (submitResult) {
            is com.babylon.wallet.android.domain.common.Result.Error -> {
                Result.failure(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.SubmitNotarizedTransaction,
                        e = submitResult.exception,
                    )
                )
            }
            is com.babylon.wallet.android.domain.common.Result.Success -> {
                // Invalidate all cached information stored, since a transaction may mutate
                // some resource information
                cache.invalidate()

                if (submitResult.data.duplicate) {
                    Result.failure(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.InvalidTXDuplicate(
                                txID
                            )
                        )
                    )
                } else {
                    Result.success(txID)
                }
            }
        }
    }
}
