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
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionStatusClient @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val _transactionPollResult = MutableStateFlow(emptyList<TransactionData>())
    private val transactionStatuses = _transactionPollResult.asSharedFlow()
    private val mutex = Mutex()

    fun listenForPollStatus(txId: String): Flow<TransactionData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.txId == txId }
        }.filterNotNull().cancellable()
    }

    fun listenForPollStatusByRequestId(requestId: String): Flow<TransactionData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.requestId == requestId }
        }.filterNotNull().cancellable()
    }

    @Suppress("MagicNumber")
    fun pollTransactionStatus(txID: String, requestId: String, transactionType: TransactionType = TransactionType.Generic) {
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
                    updateTransactionStatus(
                        TransactionData(
                            txID, requestId, kotlin.Result.failure(
                                DappRequestException(
                                    DappRequestFailure.TransactionApprovalFailure.FailedToPollTXStatus(
                                        txID
                                    )
                                )
                            ), transactionType = transactionType
                        )
                    )
                    break
                }
                delay(delayBetweenTriesMs)
            }
            if (transactionStatus.isFailed()) {
                when (transactionStatus) {
                    TransactionStatus.committedFailure -> {
                        updateTransactionStatus(
                            TransactionData(
                                txID, requestId, kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.GatewayCommittedFailure(
                                            txID
                                        )
                                    )
                                ), transactionType = transactionType
                            )
                        )
                    }

                    TransactionStatus.rejected -> {
                        updateTransactionStatus(
                            TransactionData(
                                txID, requestId, kotlin.Result.failure(
                                    DappRequestException(
                                        DappRequestFailure.TransactionApprovalFailure.GatewayRejected(txID)
                                    )
                                ), transactionType = transactionType
                            )
                        )
                    }

                    else -> {}
                }
            }
            updateTransactionStatus(
                TransactionData(
                    txID, requestId, kotlin.Result.success(Unit), transactionType = transactionType
                )
            )
        }
    }

    fun statusHandled(txId: String) {
        appScope.launch {
            mutex.withLock {
                _transactionPollResult.update { statuses ->
                    statuses.filter { it.txId != txId }
                }
            }
        }
    }

    private suspend fun updateTransactionStatus(data: TransactionData) {
        mutex.withLock {
            _transactionPollResult.update { statuses ->
                if (statuses.find { data.txId == it.txId } != null) {
                    statuses.map {
                        if (data.txId == it.txId) {
                            data
                        } else {
                            it
                        }
                    }
                } else {
                    statuses + listOf(data)
                }
            }
        }
    }

}

data class TransactionData(
    val txId: String,
    val requestId: String,
    val result: kotlin.Result<Unit>,
    val transactionType: TransactionType = TransactionType.Generic
)