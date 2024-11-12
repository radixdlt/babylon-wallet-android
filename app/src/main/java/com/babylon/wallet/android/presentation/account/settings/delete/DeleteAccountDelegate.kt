package com.babylon.wallet.android.presentation.account.settings.delete

import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.Event
import com.babylon.wallet.android.presentation.account.settings.AccountSettingsViewModel.State
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegateWithEvents
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

interface DeleteAccountDelegate {

    fun onDeleteAccountRequest()

    fun onDeleteConfirm()

}

class DeleteAccountDelegateImpl @Inject constructor(
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
) : DeleteAccountDelegate, ViewModelDelegateWithEvents<State, Event>() {
    override fun onDeleteAccountRequest() {
        _state.update { it.copy(bottomSheetContent = State.BottomSheetContent.DeleteAccount()) }
    }

    override fun onDeleteConfirm() {
        val account = _state.value.account ?: return
        viewModelScope.launch {
            _state.update { it.copy(bottomSheetContent = State.BottomSheetContent.DeleteAccount(isFetchingAssets = true)) }

            getWalletAssetsUseCase.collect(accounts = listOf(account), isRefreshing = false)
                .onSuccess { accountsWithAssets ->
                    val ownedAssets = accountsWithAssets.firstOrNull()?.assets?.ownedAssets

                    if (!ownedAssets.isNullOrEmpty()) {
                        sendEvent(Event.ChooseAccountToTransferAssetsBeforeDelete(
                            deletingAccount = account,
                            assets = ownedAssets
                        ))
                    } else {
                        // TODO construct manifest without sending account and assets
                    }
                    _state.update { it.copy(bottomSheetContent = State.BottomSheetContent.None) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            bottomSheetContent = State.BottomSheetContent.None,
                            error = UiMessage.ErrorMessage(error)
                        )
                    }
                }
        }
    }

}