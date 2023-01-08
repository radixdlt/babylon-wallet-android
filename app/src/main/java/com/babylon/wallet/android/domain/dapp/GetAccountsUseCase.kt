package com.babylon.wallet.android.domain.dapp

import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.domain.usecase.wallet.GetAccountResourcesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.repository.ProfileRepository
import timber.log.Timber
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val getAccountResourcesUseCase: GetAccountResourcesUseCase
) {

    operator fun invoke(): Flow<List<AccountResources>> {
        return profileRepository.profileSnapshot
            .filterNotNull()
            .map { profileSnapshot ->
                profileSnapshot.toProfile().getAccounts()
            }
            .map { accounts ->
                coroutineScope {
                    accounts.map { account ->
                        async {
                            getAccountResourcesUseCase(account.entityAddress.address)
                        }
                    }.awaitAll()
                }
            }
            .map { accountsResources ->
                val accountResourcesList = mutableListOf<AccountResources>()
                accountsResources.forEach { accountResources ->
                    accountResources.onValue {
                        accountResourcesList.add(it)
                    }
                }
                accountResourcesList
            }
            .catch { exception ->
                Timber.e("failed to get accounts with exception: ${exception.localizedMessage}")
                emptyList<AccountResources>()
            }
    }
}
