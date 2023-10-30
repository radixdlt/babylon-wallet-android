package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import kotlinx.coroutines.flow.map
import rdx.works.profile.data.model.pernetwork.Network
import javax.inject.Inject

class GetAccountAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    operator fun invoke(account: Network.Account, isRefreshing: Boolean) = stateRepository.observeAccountsOnLedger(
        accounts = listOf(account),
        isRefreshing = isRefreshing
    ).map { accountsOnLedger ->
        val accountOnLedger = accountsOnLedger[account]

        AccountWithAssets(
            account = account,
            details = accountOnLedger?.details,
            assets = accountOnLedger?.extractAssets()
        )
    }

}
