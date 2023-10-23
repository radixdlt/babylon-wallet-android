package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPayloadStatus
import com.babylon.wallet.android.data.repository.TransactionData
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.domain.common.Result
import javax.inject.Inject

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        txProcessingTime: String
    ): TransactionData {
        return pollTransactionStatus(txID, requestId, transactionType, txProcessingTime)
    }

    @Suppress("ReturnCount", "LongMethod")
    private suspend fun pollTransactionStatus(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        txProcessingTime: String
    ): TransactionData {
        while (true) {
            val statusCheckResult = transactionRepository.getTransactionStatus(txID)
            if (statusCheckResult is Result.Success) {
                when (statusCheckResult.data.knownPayloads.firstOrNull()?.payloadStatus) {
                    TransactionPayloadStatus.unknown -> {
                        // Keep Polling
                    }
                    TransactionPayloadStatus.committedSuccess -> {
                        // Stop Polling: MESSAGE 1
                        return TransactionData(
                            txId = txID,
                            requestId = requestId,
                            result = kotlin.Result.success(TransactionPayloadStatus.committedSuccess),
                            transactionType = transactionType,
                            txProcessingTime = txProcessingTime
                        )
                    }
                    TransactionPayloadStatus.committedFailure -> {
                        // Stop Polling: MESSAGE 2
                        return TransactionData(
                            txId = txID,
                            requestId = requestId,
                            result = kotlin.Result.failure(
                                DappRequestException(
                                    DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(
                                        txID
                                    )
                                )
                            ),
                            transactionType = transactionType,
                            txProcessingTime = txProcessingTime
                        )
                    }
                    TransactionPayloadStatus.commitPendingOutcomeUnknown -> {
                        // Keep polling
                    }
                    TransactionPayloadStatus.permanentlyRejected -> {
                        // Stop Polling: MESSAGE 4
                        return TransactionData(
                            txId = txID,
                            requestId = requestId,
                            result = kotlin.Result.failure(
                                DappRequestException(
                                    DappRequestFailure.TransactionApprovalFailure.GatewayPermanentlyRejected(
                                        txID
                                    )
                                )
                            ),
                            transactionType = transactionType,
                            txProcessingTime = txProcessingTime
                        )
                    }
                    TransactionPayloadStatus.temporarilyRejected -> {
                        // Stop Polling: MESSAGE 3
                        return TransactionData(
                            txId = txID,
                            requestId = requestId,
                            result = kotlin.Result.failure(
                                DappRequestException(
                                    DappRequestFailure.TransactionApprovalFailure.GatewayTemporarilyRejected(
                                        txID
                                    )
                                )
                            ),
                            transactionType = transactionType,
                            txProcessingTime = txProcessingTime
                        )
                    }
                    TransactionPayloadStatus.pending -> {
                        // Keep polling
                    }
                    else -> {}
                }
            }
        }
    }
}
