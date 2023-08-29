package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.data.gateway.generated.models.TransactionStatus
import com.babylon.wallet.android.data.gateway.isComplete
import com.babylon.wallet.android.data.gateway.isFailed
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class TransactionStatusClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val _transactionStatuses = MutableStateFlow<List<TransactionData>>(emptyList())
    private val transactionStatuses = _transactionStatuses.asSharedFlow()
    private val mutex = Mutex()

    fun listenForPollStatus(txId: String): Flow<TransactionData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.txId == txId }
        }.filterNotNull()
    }

    @Suppress("MagicNumber")
    fun pollTransactionStatus(txID: String) {
        appScope.launch {
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
                    updateStatus(
                        TransactionData(
                            txID, kotlin.Result.failure(
                                DappRequestException(
                                    DappRequestFailure.TransactionApprovalFailure.FailedToPollTXStatus(
                                        txID
                                    )
                                )
                            )
                        )
                    )
                }
                delay(delayBetweenTriesMs)
            }
            if (transactionStatus.isFailed()) {
                when (transactionStatus) {
                    TransactionStatus.committedFailure -> {
                        updateStatus(
                            TransactionData(
                                txID, kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(
                                            txID
                                        )
                                    )
                                )
                            )
                        )
                    }

                    TransactionStatus.rejected -> {
                        updateStatus(
                            TransactionData(
                                txID, kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.GatewayRejected(txID)
                                    )
                                )
                            )
                        )
                    }
                    else -> {}
                }
            }
            updateStatus(
                TransactionData(
                    txID, kotlin.Result.success(Unit)
                )
            )
        }
    }

    fun statusHandled(txId: String) {
        appScope.launch {
            mutex.withLock {
                _transactionStatuses.update { state ->
                    state.filter { it.txId != txId }
                }
            }
        }
    }

    private fun updateStatus(data: TransactionData) {
        appScope.launch {
            mutex.withLock {
                _transactionStatuses.update { state ->
                    (state + listOf(data)).distinctBy { it.txId }
                }
            }
        }
    }

}

data class TransactionData(
    val txId: String,
    val result: kotlin.Result<Unit>,
    val transactionType: TransactionType = TransactionType.Generic
)