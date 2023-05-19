package com.babylon.wallet.android.presentation.transfer.assets

import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network

class AssetsChooserDelegate(
    private val state: MutableStateFlow<TransferViewModel.State>,
    private val viewModelScope: CoroutineScope,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase
) {

    fun onChooseAssets(fromAccount: Network.Account, selectedAssets: Set<Resource>) {
        state.update {
            it.copy(sheet = Sheet.ChooseAssets(selectedResources = selectedAssets))
        }

        viewModelScope.launch {
            getAccountsWithResourcesUseCase(accounts = listOf(fromAccount), isRefreshing = false)
                .onValue { accountWithResources ->
                    val resources = accountWithResources.firstOrNull()?.resources
                    updateSheetState { it.copy(resources = resources) }
                }.onError { error ->
                    updateSheetState {
                        it.copy(
                            resources = Resources.EMPTY,
                            uiMessage = UiMessage.ErrorMessage(error)
                        )
                    }
                }
        }
    }


    fun onTabSelected(tab: Sheet.ChooseAssets.Tab) {
        updateSheetState { it.copy(selectedTab = tab) }
    }

    fun onAssetSelectionChanged(resource: Resource, isChecked: Boolean) {
        updateSheetState { state ->
            val selected = state.selectedResources.toMutableSet()
            if (isChecked) {
                selected.add(resource)
            } else {
                selected.remove(resource)
            }

            state.copy(selectedResources = selected.toSet())
        }
    }

    fun onUiMessageShown() {
        updateSheetState { it.copy(uiMessage = null) }
    }

    private fun updateSheetState(
        onUpdate: (Sheet.ChooseAssets) -> Sheet.ChooseAssets
    ) {
        state.update { state ->
            if (state.sheet is Sheet.ChooseAssets) {
                state.copy(sheet = onUpdate(state.sheet))
            } else {
                state
            }
        }
    }
}
