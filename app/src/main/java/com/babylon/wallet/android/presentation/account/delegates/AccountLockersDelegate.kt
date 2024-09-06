package com.babylon.wallet.android.presentation.account.delegates

import com.babylon.wallet.android.data.repository.locker.AccountLockerRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.presentation.account.AccountViewModel
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.LockerAddress
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AccountLockersDelegate @Inject constructor(
    private val accountLockersObserver: AccountLockersObserver,
    private val accountLockersRepository: AccountLockerRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModelDelegate<AccountViewModel.State>() {

    fun observeAccountLockers(accountAddress: AccountAddress) {
        viewModelScope.launch {
            accountLockersObserver.depositsByAccount()
                .onEach { accountWithLockerClaims ->
                    _state.update {
                        it.copy(
                            deposits = accountWithLockerClaims[accountAddress]
                                .orEmpty()
                                .toPersistentList()
                        )
                    }
                }
                .flowOn(defaultDispatcher)
                .collect()
        }
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
}
