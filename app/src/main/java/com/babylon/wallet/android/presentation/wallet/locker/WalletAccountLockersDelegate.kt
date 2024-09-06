package com.babylon.wallet.android.presentation.wallet.locker

import com.babylon.wallet.android.data.repository.locker.AccountLockerRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.wallet.WalletViewModel.State
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletAccountLockersDelegate @Inject constructor(
    private val accountLockersObserver: AccountLockersObserver,
    private val accountLockersRepository: AccountLockerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModelDelegate<State>() {

    override operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<State>
    ) {
        super.invoke(scope, state)
        observeAccountLockers()
    }

    fun onLockerDepositClick(
        accountAddress: AccountAddress,
        lockerAddress: LockerAddress
    ) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                accountLockersRepository.sendClaimAccountLockerDepositRequest(
                    accountAddress = accountAddress,
                    lockerAddress = lockerAddress
                )
            }
        }
    }

    private fun observeAccountLockers() {
        viewModelScope.launch {
            accountLockersObserver.depositsByAccount()
                .onEach { accountWithLockerClaims ->
                    _state.update {
                        it.copy(
                            accountsWithLockerDeposits = accountWithLockerClaims
                        )
                    }
                }
                .flowOn(defaultDispatcher)
                .collect()
        }
    }
}
