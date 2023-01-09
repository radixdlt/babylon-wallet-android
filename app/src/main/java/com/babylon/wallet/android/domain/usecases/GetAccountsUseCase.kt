package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.domain.model.AccountSlim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Account
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {

    operator fun invoke(): Flow<List<AccountSlim>> {
        return profileRepository.profileSnapshot
            .filterNotNull()
            .map { profileSnapshot ->
                profileSnapshot
                    .toProfile()
                    .getAccounts()
                    .map { account ->
                        AccountSlim(
                            address = account.entityAddress.address,
                            appearanceID = account.appearanceID,
                            displayName = account.displayName
                        )
                    }
            }
            .catch { exception ->
                Timber.e("failed to get accounts with exception: ${exception.localizedMessage}")
                emptyList<Account>()
            }
    }
}
