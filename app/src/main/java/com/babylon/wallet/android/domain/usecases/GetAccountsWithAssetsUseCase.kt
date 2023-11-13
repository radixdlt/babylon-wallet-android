package com.babylon.wallet.android.domain.usecases

import com.babylon.wallet.android.data.repository.entity.EntityRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetAccountsWithAssetsUseCase @Inject constructor(
    private val entityRepository: EntityRepository
) {

    suspend operator fun invoke(
        accounts: List<Network.Account>,
        isNftItemDataNeeded: Boolean = true,
        isRefreshing: Boolean,
    ): Result<List<AccountWithAssets>> {
        return entityRepository.getAccountsWithAssets(
            accounts = accounts,
            isNftItemDataNeeded = isNftItemDataNeeded,
            isRefreshing = isRefreshing
        )
    }
}
