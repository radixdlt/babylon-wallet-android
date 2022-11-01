package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.data.dapp.DAppAccountUiState
import com.babylon.wallet.android.domain.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend fun getAccounts(): List<DAppAccountUiState> = profileRepository.getAccounts().map { account ->
        DAppAccountUiState(account, false)
    }
}
