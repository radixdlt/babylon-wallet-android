package com.babylon.wallet.android.presentation.account.settings.delete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
) : StateViewModel<DeleteAccountViewModel.State>(),
    OneOffEventHandler<DeleteAccountViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(accountAddress = DeleteAccountArgs(savedStateHandle).accountAddress)

    fun onDeleteConfirm() {
        viewModelScope.launch {
            val account =
                getProfileUseCase().activeAccountsOnCurrentNetwork.find { it.address == state.value.accountAddress } ?: return@launch

            _state.update { it.copy(isFetchingAssets = true) }
            getWalletAssetsUseCase.collect(listOf(account), isRefreshing = false).onSuccess { accountsWithAssets ->
                val assets = accountsWithAssets.firstOrNull()?.assets?.ownedAssets.orEmpty()

                _state.update { it.copy(isFetchingAssets = false) }
                if (assets.isEmpty()) {
                    // TODO create manifest without resources
                } else {
                    sendEvent(Event.MoveAssetsToAnotherAccount(deletingAccountAddress = state.value.accountAddress))
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isFetchingAssets = false,
                        uiMessage = UiMessage.ErrorMessage(error)
                    )
                }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val accountAddress: AccountAddress,
        val isFetchingAssets: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState

    sealed interface Event : OneOffEvent {
        data class MoveAssetsToAnotherAccount(val deletingAccountAddress: AccountAddress) : Event
    }
}