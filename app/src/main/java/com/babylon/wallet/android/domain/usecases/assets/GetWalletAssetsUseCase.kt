package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository
) {

    operator fun invoke(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> {
        return stateRepository.observeAccountsOnLedger(
            accounts = accounts,
            isRefreshing = isRefreshing
        )
    }
}
