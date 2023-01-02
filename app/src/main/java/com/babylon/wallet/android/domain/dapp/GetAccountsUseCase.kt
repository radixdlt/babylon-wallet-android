package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.dapp.account.SelectedAccountUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dAppRepository: DAppRepository,
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase
) {
    operator fun invoke(
        scope: CoroutineScope
    ): Flow<Result<DAppAccountsResult>> {
        return profileRepository.profileSnapshot.filterNotNull().map {
            when (val result = dAppRepository.verifyDApp()) {
                is Result.Success -> {
                    val accounts = profileRepository.readProfileSnapshot()?.toProfile()?.getAccounts().orEmpty()
                    val accountsResources = accounts.map { account ->
                        scope.async {
                            getAccountResourcesUseCase(account.entityAddress.address)
                        }
                    }.awaitAll()

                    val uiStateAccounts = mutableListOf<SelectedAccountUiState>()
                    accountsResources.forEach { accountResources ->
                        accountResources.onValue { accountResource ->
                            uiStateAccounts.add(
                                SelectedAccountUiState(
                                    accountName = accountResource.displayName,
                                    accountAddress = accountResource.address,
                                    accountCurrency = accountResource.currencySymbol,
                                    accountValue = accountResource.value,
                                    appearanceID = accountResource.appearanceID
                                )
                            )
                        }
                    }

                    Result.Success(
                        DAppAccountsResult(
                            accounts = uiStateAccounts,
                            dAppResult = result.data
                        )
                    )
                }
                is Result.Error -> {
                    Result.Error(result.exception)
                }
            }
        }
    }
}

data class DAppAccountsResult(
    val accounts: List<SelectedAccountUiState>,
    val dAppResult: DAppResult
)
