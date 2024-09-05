package com.babylon.wallet.android.domain.utils

import com.babylon.wallet.android.data.repository.locker.AccountLockerRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.locker.AccountLockerDeposit
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.profile.data.repository.DAppConnectionRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class AccountLockersObserver @Inject constructor(
    private val getDAppsUseCase: GetDAppsUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val accountLockerRepository: AccountLockerRepository,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    private val claimsByAccount = MutableSharedFlow<Map<AccountAddress, List<AccountLockerDeposit>>>(1)

    init {
        startObserving()
    }

    fun claimsByAccount() = claimsByAccount.asSharedFlow()

    private fun startObserving() {
        appScope.launch {
            combine(
                dAppConnectionRepository.getAuthorizedDApps()
                    .distinctUntilChanged(),
                ticker()
            ) { authorizedDApps, _ ->
                checkClaims(authorizedDApps)
            }
                .onEach { claimsByAccount.emit(it) }
                .catch { Timber.w(it) }
                .flowOn(defaultDispatcher)
                .collect()
        }
    }

    private suspend fun checkClaims(
        authorizedDApps: List<AuthorizedDapp>
    ): Map<AccountAddress, List<AccountLockerDeposit>> {
        val dAppsWithAccountLockers = getDAppWithAccountLockers(authorizedDApps)
        val lockersPerUserAccount = getLockersPerUserAccount(dAppsWithAccountLockers, authorizedDApps)

        return lockersPerUserAccount.mapValues { entry ->
            val availableClaims = accountLockerRepository.getAvailableAccountLockerClaims(
                accountAddress = entry.key,
                lockerAddresses = entry.value.mapNotNull { it.lockerAddress }.toSet()
            ).getOrNull().orEmpty()

            entry.value.filter { it.lockerAddress in availableClaims }
                .mapNotNull { dAppWithClaimableItems ->
                    AccountLockerDeposit(
                        dAppName = dAppWithClaimableItems.name.orEmpty(),
                        lockerAddress = dAppWithClaimableItems.lockerAddress ?: return@mapNotNull null
                    )
                }
        }
    }

    private suspend fun getDAppWithAccountLockers(
        authorizedDApps: List<AuthorizedDapp>
    ): List<DApp> {
        val dAppDefinitionAddresses = authorizedDApps.map { it.dappDefinitionAddress }.toSet()
        return getDAppsUseCase(dAppDefinitionAddresses, true)
            .getOrNull()
            ?.filter { it.lockerAddress != null }
            .orEmpty()
    }

    private fun getLockersPerUserAccount(
        dAppsWithAccountLockers: List<DApp>,
        authorizedDApps: List<AuthorizedDapp>
    ): Map<AccountAddress, List<DApp>> {
        val dAppsWithLockerPerUserAccount = mutableMapOf<AccountAddress, List<DApp>>()

        dAppsWithAccountLockers.forEach { dAppWithAccountLocker ->
            val authorizedDApp = authorizedDApps.find { it.dappDefinitionAddress == dAppWithAccountLocker.dAppAddress }
                ?: return@forEach

            authorizedDApp.referencesToAuthorizedPersonas.map { persona -> persona.sharedAccounts?.ids.orEmpty() }
                .flatten()
                .forEach { accountAddress ->
                    val dApps = dAppsWithLockerPerUserAccount[accountAddress] ?: emptySet()
                    dAppsWithLockerPerUserAccount[accountAddress] = dApps + dAppWithAccountLocker
                }
        }

        return dAppsWithLockerPerUserAccount
    }

    private fun ticker(): Flow<Unit> {
        return flow {
            while (true) {
                emit(Unit)
                delay(5.toDuration(DurationUnit.MINUTES).inWholeMilliseconds)
            }
        }
    }
}
