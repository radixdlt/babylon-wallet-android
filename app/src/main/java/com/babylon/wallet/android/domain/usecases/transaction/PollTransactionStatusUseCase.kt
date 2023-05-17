package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.cache.HttpCache
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.Result
import kotlinx.coroutines.delay
import javax.inject.Inject

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val cache: HttpCache
) {

    suspend operator fun invoke(
        txID: String,
    ): Result<String> {
        return pollTransactionStatus(txID)
    }

    @Suppress("MagicNumber", "ReturnCount")
    suspend fun pollTransactionStatus(txID: String): Result<String> {
        var transactionStatus = TransactionStatus.pending
        var tryCount = 0
        var errorCount = 0
        val maxTries = 20
        val delayBetweenTriesMs = 2000L
        while (!transactionStatus.isComplete()) {
            tryCount++
            val statusCheckResult = transactionRepository.getTransactionStatus(txID)
            if (statusCheckResult is Result.Success) {
                transactionStatus = statusCheckResult.data.status
            } else {
                errorCount++
            }
            if (tryCount > maxTries) {
                return Result.Error(
                    DappRequestException(
                        DappRequestFailure.TransactionApprovalFailure.FailedToPollTXStatus(
                            txID
                        )
                    )
                )
            }
            delay(delayBetweenTriesMs)
        }
        if (transactionStatus.isFailed()) {
            when (transactionStatus) {
                TransactionStatus.committedFailure -> {
                    return Result.Error(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(
                                txID
                            )
                        )
                    )
                }
                TransactionStatus.rejected -> {
                    return Result.Error(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.GatewayRejected(txID)
                        )
                    )
                }
                else -> {}
            }
        }
        return Result.Success(txID)
    }
}
