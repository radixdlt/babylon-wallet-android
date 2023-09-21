package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.TransactionData
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.Result
import kotlinx.coroutines.delay
import javax.inject.Inject

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic
    ): TransactionData {
        return pollTransactionStatus(txID, requestId, transactionType)
    }

    @Suppress("MagicNumber", "LongMethod")
    private suspend fun pollTransactionStatus(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic
    ): TransactionData {
        var result = TransactionData(
            txID,
            requestId,
            kotlin.Result.success(Unit),
            transactionType = transactionType
        )
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
                result = TransactionData(
                    txID,
                    requestId,
                    kotlin.Result.failure(
                        DappRequestException(
                            DappRequestFailure.TransactionApprovalFailure.FailedToPollTXStatus(
                                txID
                            )
                        )
                    ),
                    transactionType = transactionType
                )
                break
            }
            delay(delayBetweenTriesMs)
        }
        if (transactionStatus.isFailed()) {
            when (transactionStatus) {
                TransactionStatus.committedFailure -> {
                    result = TransactionData(
                        txID,
                        requestId,
                        kotlin.Result.failure(
                            DappRequestException(
                                DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(
                                    txID
                                )
                            )
                        ),
                        transactionType = transactionType
                    )
                }

                else -> {
                    result = TransactionData(
                        txID,
                        requestId,
                        kotlin.Result.failure(
                            DappRequestException(
                                DappRequestFailure.TransactionApprovalFailure.GatewayRejected(txID)
                            )
                        ),
                        transactionType = transactionType
                    )
                }
            }
        }
        return result
    }
}
