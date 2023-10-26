package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPayloadStatus
import com.babylon.wallet.android.data.repository.TransactionStatusData
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import javax.inject.Inject

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    @Suppress("LongMethod", "ReturnCount")
    suspend operator fun invoke(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        txProcessingTime: String
    ): TransactionStatusData {
        while (true) {
            transactionRepository.getTransactionStatus(txID)
                .onSuccess { statusCheckResult ->
                    when (statusCheckResult.knownPayloads.firstOrNull()?.payloadStatus) {
                        TransactionPayloadStatus.unknown,
                        TransactionPayloadStatus.commitPendingOutcomeUnknown,
                        TransactionPayloadStatus.pending -> {
                            // Keep Polling
                        }
                        TransactionPayloadStatus.committedSuccess -> {
                            // Stop Polling: MESSAGE 1
                            return TransactionStatusData(
                                txId = txID,
                                requestId = requestId,
                                result = kotlin.Result.success(Unit),
                                transactionType = transactionType,
                                txProcessingTime = txProcessingTime
                            )
                        }
                        TransactionPayloadStatus.committedFailure -> {
                            // Stop Polling: MESSAGE 2
                            return TransactionStatusData(
                                txId = txID,
                                requestId = requestId,
                                result = kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.TransactionCommitted.Failure(
                                            txID
                                        )
                                    )
                                ),
                                transactionType = transactionType,
                                txProcessingTime = txProcessingTime
                            )
                        }
                        TransactionPayloadStatus.permanentlyRejected -> {
                            // Stop Polling: MESSAGE 4
                            return TransactionStatusData(
                                txId = txID,
                                requestId = requestId,
                                result = kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.TransactionRejected.Permanently(
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
                            return TransactionStatusData(
                                txId = txID,
                                requestId = requestId,
                                result = kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.TransactionRejected.Temporary(
                                            txID
                                        )
                                    )
                                ),
                                transactionType = transactionType,
                                txProcessingTime = txProcessingTime
                            )
                        }
                        null -> {}
                    }
                }
        }
    }
}
