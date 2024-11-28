package com.babylon.wallet.android.domain.usecases.transaction

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.repository.TransactionStatusData
import com.babylon.wallet.android.di.coroutines.IoDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.utils.toMinutes
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.TransactionStatus
import com.radixdlt.sargon.TransactionStatusReason
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import rdx.works.core.mapError
import javax.inject.Inject

class GetTransactionStatusUseCase @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke(
        intentHash: TransactionIntentHash,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        endEpoch: Epoch
    ): TransactionStatusData = withContext(dispatcher) {
        val txId = intentHash.bech32EncodedTxId

        runCatching {
            val sargonOs = sargonOsManager.sargonOs

            when (val transactionStatus = sargonOs.pollTransactionStatus(intentHash)) {
                is TransactionStatus.Failed -> TransactionStatusData(
                    txId = txId,
                    requestId = requestId,
                    result = TransactionStatusData.Status.Failed(
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
                    result = TransactionStatusData.Status.Failed(
                        RadixWalletException.TransactionSubmitException.TransactionRejected.Permanently(txId)
                    ),
                    transactionType = transactionType
                )
                TransactionStatus.Success -> TransactionStatusData(
                    txId = txId,
                    requestId = requestId,
                    result = TransactionStatusData.Status.Success,
                    transactionType = transactionType
                )
                is TransactionStatus.TemporarilyRejected -> TransactionStatusData(
                    txId = txId,
                    requestId = requestId,
                    result = TransactionStatusData.Status.Failed(
                        RadixWalletException.TransactionSubmitException.TransactionRejected.Temporary(
                            txId = txId,
                            txProcessingTime = (endEpoch - transactionStatus.currentEpoch).toMinutes().toString()
                        )
                    ),
                    transactionType = transactionType
                )
            }
        }.mapError { RadixWalletException.TransactionSubmitException.FailedToPollTXStatus(txId) }.getOrThrow()
    }
}
