package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    operator fun invoke(accounts: List<Network.Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
        return stateRepository.observeAccountsOnLedger(
            accounts = accounts,
            isRefreshing = isRefreshing
        )
    }
}
