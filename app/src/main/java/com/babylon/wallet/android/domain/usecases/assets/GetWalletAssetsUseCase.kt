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

    fun observe(
        accounts: List<Account>,
        isRefreshing: Boolean,
        includeHiddenResources: Boolean = false
    ): Flow<List<AccountWithAssets>> = stateRepository.observeAccountsOnLedger(
        accounts = accounts,
        isRefreshing = isRefreshing,
        includeHiddenResources = includeHiddenResources
    )

    suspend fun collect(
        accounts: List<Account>,
        isRefreshing: Boolean,
        includeHiddenResources: Boolean = false
    ): Result<List<AccountWithAssets>> = observe(
        accounts = accounts,
        isRefreshing = isRefreshing,
        includeHiddenResources = includeHiddenResources
    ).map {
        Result.success(it)
    }.catch { error ->
        emit(Result.failure(error))
    }.first()

    suspend fun collect(
        account: Account,
        isRefreshing: Boolean,
        includeHiddenResources: Boolean = false
    ): Result<AccountWithAssets> = collect(
        accounts = listOf(account),
        isRefreshing = isRefreshing,
        includeHiddenResources = includeHiddenResources
    ).mapCatching { it.first() }
}
