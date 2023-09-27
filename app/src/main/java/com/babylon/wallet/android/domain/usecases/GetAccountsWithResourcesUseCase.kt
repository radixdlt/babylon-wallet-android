package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.model.AccountWithResources
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class GetAccountsWithResourcesUseCase @Inject constructor(
    private val entityRepository: EntityRepository,
    private val getProfileUseCase: GetProfileUseCase
) {

    suspend operator fun invoke(
        accounts: List<Network.Account>,
        isNftItemDataNeeded: Boolean = true,
        isRefreshing: Boolean,
    ): Result<List<AccountWithResources>> {
        return entityRepository.getAccountsWithResources(
            accounts = accounts,
            isNftItemDataNeeded = isNftItemDataNeeded,
            isRefreshing = isRefreshing
        )
    }

    suspend operator fun invoke(
        isNftItemDataNeeded: Boolean,
        isRefreshing: Boolean
    ): Result<List<AccountWithResources>> {
        val accounts = getProfileUseCase.accountsOnCurrentNetwork()
        return invoke(accounts = accounts, isNftItemDataNeeded = isNftItemDataNeeded, isRefreshing = isRefreshing)
    }
}
