package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.locker.AccountLockerRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.LockerAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import rdx.works.core.domain.DApp
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject

class GetAccountsWithLockerClaimStatusesUseCase @Inject constructor(
    private val getDAppsUseCase: GetDAppsUseCase,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val accountLockerRepository: AccountLockerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    operator fun invoke(): Flow<Map<AccountAddress, List<AccountLockerClaim>>> {
        return dAppConnectionRepository.getAuthorizedDApps()
            .distinctUntilChanged()
            .map { authorizedDApps ->
                val dAppsWithAccountLockers = getDAppWithAccountLockers(authorizedDApps)
                val lockersPerUserAccount = getLockersPerUserAccount(dAppsWithAccountLockers, authorizedDApps)

                lockersPerUserAccount.mapValues { entry ->
                    val availableClaims = accountLockerRepository.getAvailableAccountLockerClaims(
                        accountAddress = entry.key,
                        lockerAddresses = entry.value.mapNotNull { it.lockerAddress }.toSet()
                    ).getOrNull().orEmpty()

                    entry.value.filter { it.lockerAddress in availableClaims }
                        .mapNotNull { dAppWithClaimableItems ->
                            AccountLockerClaim(
                                dAppName = dAppWithClaimableItems.name.orEmpty(),
                                lockerAddress = dAppWithClaimableItems.lockerAddress ?: return@mapNotNull null
                            )
                        }
                }
            }.flowOn(defaultDispatcher)
    }

    private suspend fun getDAppWithAccountLockers(authorizedDApps: List<AuthorizedDapp>): List<DApp> {
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

    data class AccountLockerClaim(
        val lockerAddress: LockerAddress,
        val dAppName: String
    )
}
