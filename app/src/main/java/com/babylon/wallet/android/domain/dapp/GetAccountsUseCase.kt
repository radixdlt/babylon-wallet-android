package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import com.babylon.wallet.android.presentation.dapp.account.SelectedAccountUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dAppRepository: DAppRepository,
    private val requestAccountResourcesUseCase: GetAccountResourcesUseCase
) {
    suspend operator fun invoke(
        scope: CoroutineScope
    ): Result<DAppAccountsResult> {
        return when (val result = dAppRepository.verifyDApp()) {
            is Result.Success -> {
                val accounts = profileRepository.readProfileSnapshot()?.toProfile()?.getAccounts().orEmpty()
                val results = accounts.map { account ->
                    scope.async {
                        requestAccountResourcesUseCase(account.entityAddress.address)
                    }
                }.awaitAll()

                val uiStateAccounts = mutableListOf<SelectedAccountUiState>()
                results.forEach { accountResourcesResult ->
                    accountResourcesResult.onValue { accountResource ->
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

data class DAppAccountsResult(
    val accounts: List<SelectedAccountUiState>,
    val dAppResult: DAppResult
)
