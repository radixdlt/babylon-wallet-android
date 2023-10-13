package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(accounts: List<Network.Account>): Result<List<AccountWithResources>> {
        return stateRepository.getAccountsState(accounts).map { accountsAndResources ->
            accountsAndResources.map { entry ->
                AccountWithResources(
                    account = entry.key,
                    resources = Resources(
                        fungibleResources = entry.value.filterIsInstance<Resource.FungibleResource>(),
                        nonFungibleResources = entry.value.filterIsInstance<Resource.NonFungibleResource>(),
                        poolUnits = listOf(),
                        validatorsWithStakeResources = ValidatorsWithStakeResources()
                    )
                )
            }
        }
    }

}
