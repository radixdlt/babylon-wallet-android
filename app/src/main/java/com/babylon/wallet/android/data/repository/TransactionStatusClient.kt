package com.babylon.wallet.android.data.repository

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.TombstoneAccountUseCase
import com.babylon.wallet.android.domain.usecases.transaction.CommitProvisionalShieldUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetPreAuthorizationStatusUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionStatusUseCase
import com.babylon.wallet.android.domain.utils.AccessControllerTimedRecoveryStateObserver
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Epoch
import com.radixdlt.sargon.Instant
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

@Suppress("LongParameterList")
@Singleton
class TransactionStatusClient @Inject constructor(
    private val getTransactionStatusUseCase: GetTransactionStatusUseCase,
    private val getPreAuthorizationStatusUseCase: GetPreAuthorizationStatusUseCase,
    private val appEventBus: AppEventBus,
    private val preferencesManager: PreferencesManager,
    private val tombstoneAccountUseCase: TombstoneAccountUseCase,
    private val accessControllerTimedRecoveryStateObserver: AccessControllerTimedRecoveryStateObserver,
    private val commitProvisionalShieldUseCase: CommitProvisionalShieldUseCase,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val transactionResult = MutableStateFlow(emptyList<InteractionStatusData>())
    private val transactionStatuses = transactionResult.asSharedFlow()
    private val mutex = Mutex()

    fun listenForTransactionStatus(txId: String): Flow<TransactionStatusData> {
        return listenForStatus(txId).filterIsInstance()
    }

    fun listenForPreAuthorizationStatus(preAuthorizationId: String): Flow<PreAuthorizationStatusData> {
        return listenForStatus(preAuthorizationId).filterIsInstance()
    }

    fun listenForTransactionStatusByRequestId(requestId: String): Flow<TransactionStatusData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.requestId == requestId }
        }.filterNotNull().cancellable().filterIsInstance()
    }

    fun observeTransactionStatus(
        intentHash: TransactionIntentHash,
        requestId: String,
        transactionType: TransactionType = TransactionType.Generic,
        endEpoch: Epoch
    ) {
        appScope.launch {
            val result = getTransactionStatusUseCase(intentHash, requestId, transactionType, endEpoch)
            val isSuccess = result.result is TransactionStatusData.Status.Success

            if (isSuccess) {
                when (transactionType) {
                    is TransactionType.DeleteAccount -> {
                        // When a delete account transaction is successful, the first thing to do is to tombstone the account.
                        // Before any other update takes place in wallet.
                        tombstoneAccountUseCase(transactionType.accountAddress)
                    }

                    is TransactionType.SecurifyEntity -> {
                        // When a securify entity transaction is successful, the first thing to do is to mark it as such in profile.
                        // Before any other update takes place in wallet.
                        commitProvisionalShieldUseCase(transactionType.entityAddress)
                    }

                    is TransactionType.ConfirmSecurityStructureRecovery -> {
                        accessControllerTimedRecoveryStateObserver.startMonitoring()
                        commitProvisionalShieldUseCase(transactionType.entityAddress)
                    }

                    TransactionType.InitiateSecurityStructureRecovery -> {
                        accessControllerTimedRecoveryStateObserver.startMonitoring()
                    }

                    is TransactionType.StopSecurityStructureRecovery -> {
                        accessControllerTimedRecoveryStateObserver.startMonitoring()
                        commitProvisionalShieldUseCase(transactionType.entityAddress)
                    }

                    is TransactionType.UpdateThirdPartyDeposits,
                    TransactionType.Generic -> {
                        // Do nothing
                    }
                }
            }

            updateTransactionStatus(result)

            if (isSuccess) {
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

    fun observePreAuthorizationStatus(
        intentHash: SubintentHash,
        requestId: String,
        expiration: Instant
    ) {
        appScope.launch {
            val result = getPreAuthorizationStatusUseCase(intentHash, requestId, expiration)

            updateTransactionStatus(result)

            if (result.result is PreAuthorizationStatusData.Status.Success) {
                preferencesManager.incrementTransactionCompleteCounter()
                appEventBus.sendEvent(AppEvent.RefreshAssetsNeeded)
            }
        }
    }

    fun statusHandled(txId: String) {
        appScope.launch {
            mutex.withLock {
                transactionResult.update { statuses ->
                    statuses.filter { it.txId != txId }
                }
            }
        }
    }

    private fun listenForStatus(txId: String): Flow<InteractionStatusData> {
        return transactionStatuses.map { statuses ->
            statuses.find { it.txId == txId }
        }.filterNotNull().cancellable()
    }

    private suspend fun updateTransactionStatus(data: InteractionStatusData) {
        mutex.withLock {
            transactionResult.update { statuses ->
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
    val result: Status,
    val transactionType: TransactionType = TransactionType.Generic
) : InteractionStatusData {

    sealed interface Status {

        data object Success : Status

        data class Failed(val error: RadixWalletException) : Status

        suspend fun onSuccess(action: suspend () -> Unit): Status {
            if (this is Success) {
                action()
            }
            return this
        }

        suspend fun onFailure(action: suspend (RadixWalletException) -> Unit): Status {
            if (this is Failed) {
                action(error)
            }
            return this
        }
    }
}

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
