package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppResult
import com.babylon.wallet.android.domain.Result
import com.babylon.wallet.android.domain.profile.ProfileRepository
import com.babylon.wallet.android.presentation.dapp.account.SelectedAccountUiState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val dAppRepository: DAppRepository
) {
    suspend fun getAccountsResult(): Result<DAppAccountsResult> {
        return when (val result = dAppRepository.verifyDApp()) {
            is Result.Success -> {
                Result.Success(
                    DAppAccountsResult(
                        accounts = profileRepository.getAccounts().map { account ->
                            SelectedAccountUiState(account, false)
                        },
                        dAppResult = result.data
                    )
                )
            }
            is Result.Error -> {
                Result.Error(result.message)
            }
        }
    }
}

data class DAppAccountsResult(
    val accounts: List<SelectedAccountUiState>,
    val dAppResult: DAppResult
)
