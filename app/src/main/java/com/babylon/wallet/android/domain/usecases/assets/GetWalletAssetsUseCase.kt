package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.ValidatorsWithStakeResources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    operator fun invoke(accounts: List<Network.Account>): Flow<List<AccountWithAssets>> {
        return stateRepository.getAccountsState(accounts).map { accountsAndResources ->
            accountsAndResources.map { entry ->
                AccountWithAssets(
                    account = entry.key,
                    assets = Assets(
                        fungibles = entry.value.fungibles,
                        nonFungibles = entry.value.nonFungibles,
                        poolUnits = listOf(),
                        validatorsWithStakeResources = ValidatorsWithStakeResources()
                    )
                )
            }
        }
        .flowOn(dispatcher)
    }

}
