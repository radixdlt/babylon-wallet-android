package com.babylon.wallet.android.presentation.status.dapp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.DAppWithResources
import com.babylon.wallet.android.domain.usecases.GetDAppWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetValidatedDAppWebsiteUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.then
import javax.inject.Inject

@HiltViewModel
class DAppDetailsDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDAppWithResourcesUseCase: GetDAppWithResourcesUseCase,
    getValidatedDAppWebsiteUseCase: GetValidatedDAppWebsiteUseCase
) : StateViewModel<DAppDetailsDialogViewModel.State>() {

    private val args = DAppDetailsDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        dAppDefinitionAddress = args.dAppDefinitionAddress
    )

    init {
        viewModelScope.launch {
            getDAppWithResourcesUseCase(
                definitionAddress = args.dAppDefinitionAddress,
                needMostRecentData = false
            ).onSuccess { dAppWithResources ->
                _state.update { it.copy(dAppWithResources = dAppWithResources) }
            }.then { dAppWithResources ->
                _state.update { it.copy(isWebsiteValidating = true) }
                getValidatedDAppWebsiteUseCase(dAppWithResources.dApp).onSuccess { website ->
                    _state.update { it.copy(validatedWebsite = website, isWebsiteValidating = false) }
                }
            }.onFailure { error ->
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error), isWebsiteValidating = false) }
            }
        }
    }
    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }
    data class State(
        val dAppDefinitionAddress: String,
        val dAppWithResources: DAppWithResources? = null,
        val validatedWebsite: String? = null,
        val isWebsiteValidating: Boolean = false,
        val uiMessage: UiMessage? = null
    ) : UiState
}
