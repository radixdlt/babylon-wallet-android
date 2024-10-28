package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.repository.TransactionStatusData
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.utils.toMinutes
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.TransactionStatus
import com.radixdlt.sargon.TransactionStatusReason
import kotlinx.coroutines.delay
import javax.inject.Inject

private const val DEFAULT_POLL_INTERVAL_MS = 2000L

class PollTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(
        intentHash: TransactionIntentHash,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        endEpoch: Epoch
    ): TransactionStatusData {
        val txId = intentHash.bech32EncodedTxId

        while (true) {
            transactionRepository.pollTransactionStatus(intentHash)
                .onSuccess { transactionStatus ->
                    return when (transactionStatus) {
                        is TransactionStatus.Failed -> TransactionStatusData(
                            txId = txId,
                            requestId = requestId,
                            result = Result.failure(
                                if (transactionStatus.reason == TransactionStatusReason.WORKTOP_ERROR) {
                                    RadixWalletException.TransactionSubmitException.TransactionCommitted.AssertionFailed(txId)
                                } else {
                                    RadixWalletException.TransactionSubmitException.TransactionCommitted.Failure(txId)
                                }
                            ),
                            transactionType = transactionType
                        )
                        is TransactionStatus.PermanentlyRejected -> TransactionStatusData(
                            txId = txId,
                            requestId = requestId,
                            result = Result.failure(RadixWalletException.TransactionSubmitException.TransactionRejected.Permanently(txId)),
                            transactionType = transactionType
                        )
                        TransactionStatus.Success -> TransactionStatusData(
                            txId = txId,
                            requestId = requestId,
                            result = Result.success(Unit),
                            transactionType = transactionType
                        )
                        is TransactionStatus.TemporarilyRejected -> TransactionStatusData(
                            txId = txId,
                            requestId = requestId,
                            result = Result.failure(
                                RadixWalletException.TransactionSubmitException.TransactionRejected.Temporary(
                                    txId = txId,
                                    txProcessingTime = (endEpoch - transactionStatus.currentEpoch).toMinutes().toString()
                                )
                            ),
                            transactionType = transactionType
                        )
                    }
                }
                .onFailure {
                    // Retry after a delay
                    // It can only fail if the sargonOs is not booted yet, or if the GW request fails
                    delay(DEFAULT_POLL_INTERVAL_MS)
                }
        }
    }
}
