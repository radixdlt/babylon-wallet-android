package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.AccountDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Network
import timber.log.Timber
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) {

    operator fun invoke(accounts: List<Network.Account>): Flow<List<AccountWithAssets>> {
        return stateRepository.observeAccountsResources(accounts).map { accountsAndResources ->
            accounts.map { account ->
                val detailsAndResources = accountsAndResources[account]

                AccountWithAssets(
                    account = account,
                    details = detailsAndResources?.details ?: AccountDetails(),
                    assets = detailsAndResources?.toAssets()
                )
            }
        }
        .flowOn(dispatcher)
    }
}
