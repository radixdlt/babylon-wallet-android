package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    suspend fun collect(accounts: List<Account>, isRefreshing: Boolean): Result<List<AccountWithAssets>> =
        stateRepository.observeAccountsOnLedger(
            accounts = accounts,
            isRefreshing = isRefreshing
        ).map {
            Result.success(it)
        }.catch { error ->
            emit(Result.failure(error))
        }.first()
}
