package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPayloadStatus
import com.babylon.wallet.android.data.repository.TransactionStatusData
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.radixdlt.sargon.Epoch
import kotlinx.coroutines.delay
import javax.inject.Inject

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    @Suppress("LongMethod", "ReturnCount")
    suspend operator fun invoke(
        txID: String,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        endEpoch: Epoch
    ): TransactionStatusData {
        var defaultPollDelayMs = DEFAULT_POLL_INTERVAL_MS
        while (true) {
            transactionRepository.getTransactionStatus(txID).onSuccess { statusCheckResult ->
                val currentEpoch = statusCheckResult.ledgerState.epoch.toULong()
                val txProcessingTime = ((endEpoch - currentEpoch) * 5.toULong()).toString()

                when (statusCheckResult.knownPayloads.firstOrNull()?.payloadStatus) {
                    TransactionPayloadStatus.unknown,
                    TransactionPayloadStatus.commitPendingOutcomeUnknown,
                    TransactionPayloadStatus.pending -> {
                        delay(defaultPollDelayMs)
                        defaultPollDelayMs += POLL_INTERVAL_MS
                    }

                    TransactionPayloadStatus.committedSuccess -> {
                        // Stop Polling: MESSAGE 1
                        return TransactionStatusData(
                            txId = txID,
                            requestId = requestId,
                            result = Result.success(Unit),
                            transactionType = transactionType
                        )
                    }

                    TransactionPayloadStatus.committedFailure -> {
                        // Stop Polling: MESSAGE 2
                        val isAssertionFailure = statusCheckResult.errorMessage?.contains("AssertionFailed") == true
                        val exception = if (isAssertionFailure) {
                            RadixWalletException.TransactionSubmitException.TransactionCommitted.AssertionFailed(
                                txID
                            )
                        } else {
                            RadixWalletException.TransactionSubmitException.TransactionCommitted.Failure(
                                txID
                            )
                        }
                        return TransactionStatusData(
                            txId = txID,
                            requestId = requestId,
                            result = Result.failure(exception),
                            transactionType = transactionType
                        )
                    }

                    TransactionPayloadStatus.permanentlyRejected -> {
                        // Stop Polling: MESSAGE 4
                        return TransactionStatusData(
                            txId = txID,
                            requestId = requestId,
                            result = Result.failure(
                                RadixWalletException.TransactionSubmitException.TransactionRejected.Permanently(
                                    txID
                                )
                            ),
                            transactionType = transactionType
                        )
                    }

                    TransactionPayloadStatus.temporarilyRejected -> {
                        // Stop Polling: MESSAGE 3
                        return TransactionStatusData(
                            txId = txID,
                            requestId = requestId,
                            result = Result.failure(
                                RadixWalletException.TransactionSubmitException.TransactionRejected.Temporary(
                                    txID,
                                    txProcessingTime
                                )
                            ),
                            transactionType = transactionType
                        )
                    }

                    null -> {}
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_POLL_INTERVAL_MS = 2000L
        private const val POLL_INTERVAL_MS = 1000L
    }
}
