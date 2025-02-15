package com.babylon.wallet.android.presentation.account.settings.delete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.usecases.PrepareTransactionForAccountDeletionUseCase
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
    private val prepareTransactionForAccountDeletionUseCase: PrepareTransactionForAccountDeletionUseCase,
    private val incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<DeleteAccountViewModel.State>(),
    OneOffEventHandler<DeleteAccountViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State(accountAddress = DeleteAccountArgs(savedStateHandle).accountAddress)

    fun onDeleteConfirm() {
        viewModelScope.launch {
            val account =
                getProfileUseCase().activeAccountsOnCurrentNetwork.find { it.address == state.value.accountAddress } ?: return@launch

            _state.update { it.copy(isFetchingAssets = true) }
            getWalletAssetsUseCase.collect(
                account = account,
                isRefreshing = false,
                includeHiddenResources = true
            ).onSuccess { accountWithAssets ->
                val assets = accountWithAssets.assets?.ownedAssets.orEmpty()

                if (assets.isEmpty()) {
                    _state.update { it.copy(isFetchingAssets = false, isPreparingManifest = true) }
                    prepareTransaction()
                } else {
                    _state.update { it.copy(isFetchingAssets = false, isPreparingManifest = false) }
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

    private suspend fun prepareTransaction() {
        prepareTransactionForAccountDeletionUseCase(
            deletingAccountAddress = state.value.accountAddress
        ).onSuccess {
            incomingRequestRepository.add(it.transactionRequest)
            _state.update { state -> state.copy(isPreparingManifest = false) }
        }.onFailure { error ->
            _state.update { it.copy(isPreparingManifest = false, uiMessage = UiMessage.ErrorMessage(error)) }
        }
    }

    data class State(
        val accountAddress: AccountAddress,
        private val isFetchingAssets: Boolean = false,
        private val isPreparingManifest: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isContinueLoading: Boolean
            get() = isFetchingAssets || isPreparingManifest
    }

    sealed interface Event : OneOffEvent {
        data class MoveAssetsToAnotherAccount(val deletingAccountAddress: AccountAddress) : Event
    }
}
