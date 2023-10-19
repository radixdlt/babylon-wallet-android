package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.AccountWithAssets
import com.babylon.wallet.android.domain.model.Assets
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.ValidatorsWithStakeResources
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    suspend operator fun invoke(accounts: List<Network.Account>): Result<List<AccountWithAssets>> {
        return stateRepository.getAccountsState(accounts).map { accountsAndResources ->
            accountsAndResources.map { entry ->
                AccountWithAssets(
                    account = entry.key,
                    assets = Assets(
                        fungibles = entry.value.filterIsInstance<Resource.FungibleResource>(),
                        nonFungibles = entry.value.filterIsInstance<Resource.NonFungibleResource>(),
                        poolUnits = listOf(),
                        validatorsWithStakeResources = ValidatorsWithStakeResources()
                    )
                )
            }
        }
    }

}
