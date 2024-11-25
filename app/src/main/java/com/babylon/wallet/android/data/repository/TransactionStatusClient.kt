package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.usecases.TombstoneAccountUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollPreAuthorizationStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.TransactionIntentHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionStatusClient @Inject constructor(
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    private val pollPreAuthorizationStatusUseCase: PollPreAuthorizationStatusUseCase,
    private val appEventBus: AppEventBus,
    private val preferencesManager: PreferencesManager,
    private val tombstoneAccountUseCase: TombstoneAccountUseCase,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val _transactionPollResult = MutableStateFlow(emptyList<InteractionStatusData>())
    private val transactionStatuses = _transactionPollResult.asSharedFlow()
    private val mutex = Mutex()

    fun listenForTransactionPollStatus(txId: String): Flow<TransactionStatusData> {
        return listenForPollStatus(txId).filterIsInstance()
    }

    fun listenForPreAuthorizationPollStatus(preAuthorizationId: String): Flow<PreAuthorizationStatusData> {
        return listenForPollStatus(preAuthorizationId).filterIsInstance()
    }

    fun listenForTransactionPollStatusByRequestId(requestId: String): Flow<TransactionStatusData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.requestId == requestId }
        }.filterNotNull().cancellable().filterIsInstance()
    }

    fun startPollingForTransactionStatus(
        intentHash: TransactionIntentHash,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        endEpoch: Epoch
    ) {
        appScope.launch {
            val pollResult = pollTransactionStatusUseCase(intentHash, requestId, transactionType, endEpoch)

            pollResult.result.onSuccess {
                if (transactionType is TransactionType.DeleteAccount) {
                    // When a delete account transaction is successful, the first thing to do is to tombstone the account.
                    // Before any other update takes place in wallet.
                    tombstoneAccountUseCase(transactionType.accountAddress)
                }
            }

            updateTransactionStatus(pollResult)

            pollResult.result.onSuccess {
                preferencesManager.incrementTransactionCompleteCounter()

                if (transactionType is TransactionType.DeleteAccount) {
                    // Now that the success dialog has already been previewed, it is safe to show the deleted account success screen
                    appEventBus.sendEvent(AppEvent.AccountDeleted(transactionType.accountAddress))
                }

                // After all are done, it is safe to refresh the wallet.
                appEventBus.sendEvent(AppEvent.RefreshAssetsNeeded)
            }
        }
    }

    fun startPollingForPreAuthorizationStatus(
        intentHash: SubintentHash,
        requestId: String,
        expiration: DappToWalletInteractionSubintentExpiration
    ) {
        appScope.launch {
            val pollResult = pollPreAuthorizationStatusUseCase(intentHash, requestId, expiration)

            updateTransactionStatus(pollResult)

            if (pollResult.result is PreAuthorizationStatusData.Status.Success) {
                preferencesManager.incrementTransactionCompleteCounter()
                appEventBus.sendEvent(AppEvent.RefreshAssetsNeeded)
            }
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

    private fun listenForPollStatus(txId: String): Flow<InteractionStatusData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.txId == txId }
        }.filterNotNull().cancellable()
    }

    private suspend fun updateTransactionStatus(data: InteractionStatusData) {
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

sealed interface InteractionStatusData {

    val txId: String
    val requestId: String
}

data class TransactionStatusData(
    override val txId: String,
    override val requestId: String,
    val result: Result<Unit>,
    val transactionType: TransactionType = TransactionType.Generic
) : InteractionStatusData

data class PreAuthorizationStatusData(
    override val txId: String,
    override val requestId: String,
    val result: Status
) : InteractionStatusData {

    sealed interface Status {

        data class Success(
            val txIntentHash: TransactionIntentHash
        ) : Status

        data object Expired : Status
    }
}
