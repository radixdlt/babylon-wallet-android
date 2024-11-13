package com.babylon.wallet.android.domain.usecases.assets

import com.babylon.wallet.android.data.repository.state.StateRepository
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.TombstoneAccountsUseCase
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class GetWalletAssetsUseCase @Inject constructor(
    private val stateRepository: StateRepository,
    private val tombstoneAccountsUseCase: TombstoneAccountsUseCase,
    private val appEventBus: AppEventBus
) {

    fun observe(accounts: List<Account>, isRefreshing: Boolean): Flow<List<AccountWithAssets>> = stateRepository.observeAccountsOnLedger(
        accounts = accounts,
        isRefreshing = isRefreshing
    ).onEach { accountsWithAssets ->
        val deletedAccountAddresses = accountsWithAssets.filter { it.isAccountDeleted }.map { it.account.address }.toSet()

        if (deletedAccountAddresses.isNotEmpty()) {
            tombstoneAccountsUseCase(deletedAccountAddresses)
            appEventBus.sendEvent(AppEvent.AccountsPreviouslyDeletedDetected)
        }
    }

    suspend fun collect(accounts: List<Account>, isRefreshing: Boolean): Result<List<AccountWithAssets>> = observe(
        accounts = accounts,
        isRefreshing = isRefreshing
    ).map {
        Result.success(it)
    }.catch { error ->
        emit(Result.failure(error))
    }.first()

    suspend fun collect(account: Account, isRefreshing: Boolean): Result<AccountWithAssets> = collect(
        accounts = listOf(account),
        isRefreshing = isRefreshing
    ).mapCatching { it.first() }
}
