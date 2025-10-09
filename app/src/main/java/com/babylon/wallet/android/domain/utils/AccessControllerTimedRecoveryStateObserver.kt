package com.babylon.wallet.android.domain.utils

import com.babylon.wallet.android.data.repository.accesscontroller.AccessControllersRepository
import com.babylon.wallet.android.data.repository.accesscontroller.model.AccessControllerRecoveryState
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.securityStateAccessControllerAddress
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class AccessControllerTimedRecoveryStateObserver @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val accessControllersRepository: AccessControllersRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private var monitoringJob: Job? = null
    private val _recoveryStateByAccount = MutableSharedFlow<Map<AccountAddress, AccessControllerRecoveryState>>(1)
    val recoveryStateByAccount: Flow<Map<AccountAddress, AccessControllerRecoveryState>> =
        _recoveryStateByAccount.asSharedFlow()

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = combine(
            getProfileUseCase.flow.map { it.activeAccountsOnCurrentNetwork }
                .distinctUntilChanged(),
            ticker(5.minutes)
        ) { accounts, _ -> updateAccessControllerRecoveryStates(accounts) }
            .catch { Timber.w(it) }
            .flowOn(defaultDispatcher)
            .launchIn(appScope)
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    private suspend fun updateAccessControllerRecoveryStates(accounts: List<Account>) {
        val accessControllerAddresses = accounts.mapNotNull { account ->
            account.securityStateAccessControllerAddress ?: return@mapNotNull null
        }.toSet()

        accessControllersRepository.getAccessControllerRecoveryStates(accessControllerAddresses, false)
            .onSuccess { states ->
                _recoveryStateByAccount.emit(
                    accounts.mapNotNull { account ->
                        val accessControllerAddress = account.securityStateAccessControllerAddress
                            ?: return@mapNotNull null
                        states.firstOrNull { it.address == accessControllerAddress }
                            ?.let { state -> account.address to state }
                    }.toMap()
                )
            }.onFailure {
                Timber.w(it)
            }
    }
}
