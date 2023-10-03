package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import kotlinx.coroutines.CoroutineScope
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
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    private val appEventBus: AppEventBus,
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

    @Suppress("MagicNumber", "LongMethod")
    fun pollTransactionStatus(txID: String, requestId: String, transactionType: TransactionType = TransactionType.Generic) {
        appScope.launch {
            val pollResult = pollTransactionStatusUseCase(txID, requestId, transactionType)
            pollResult.result.onSuccess {
                appEventBus.sendEvent(AppEvent.RefreshResourcesNeeded)
            }
            updateTransactionStatus(pollResult)
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
                if (statuses.any { data.txId == it.txId }) {
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
    val result: Result<Unit>,
    val transactionType: TransactionType = TransactionType.Generic
)
