package com.babylon.wallet.android.presentation.wallet.delegates

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.utils.AccessControllerStateDetailsObserver
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.wallet.WalletViewModel.State
import com.radixdlt.sargon.AddressOfAccountOrPersona
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class WalletAccountTimedRecoveryDelegate @Inject constructor(
    private val observer: AccessControllerStateDetailsObserver,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModelDelegate<State>() {

    override operator fun invoke(
        scope: CoroutineScope,
        state: MutableStateFlow<State>
    ) {
        super.invoke(scope, state)
        observeRecoveryStates()
    }

    fun onRefresh() {
        observer.startMonitoring()
    }

    private fun observeRecoveryStates() {
        observer.acStateByEntityAddress
            .onEach { states ->
                _state.update {
                    it.copy(
                        accountsWithRecoveryStates = states.mapNotNull { entry ->
                            val address =
                                (entry.key as? AddressOfAccountOrPersona.Account)?.v1 ?: return@mapNotNull null
                            address to entry.value
                        }.toMap()
                    )
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }
}
