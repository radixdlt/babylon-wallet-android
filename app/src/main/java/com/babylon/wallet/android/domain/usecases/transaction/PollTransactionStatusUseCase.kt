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
import javax.inject.Inject

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

        return when (val transactionStatus = transactionRepository.pollTransactionStatus(intentHash)) {
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
}
