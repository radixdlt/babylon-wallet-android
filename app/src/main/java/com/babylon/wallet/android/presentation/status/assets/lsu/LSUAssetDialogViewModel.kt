package com.babylon.wallet.android.presentation.status.assets.lsu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.ValidatorWithStakes
import com.babylon.wallet.android.domain.usecases.assets.GetLSUDetailsUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolUnitDetailsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LSUAssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getLSUDetailsUseCase: GetLSUDetailsUseCase
) : StateViewModel<LSUAssetDialogViewModel.State>() {

    private val args = LSUAssetDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        resourceAddress = args.resourceAddress,
        accountAddress = args.accountAddress,
        validatorWithStakes = null
    )

    init {
        viewModelScope.launch {
            getLSUDetailsUseCase(
                resourceAddress = args.resourceAddress,
                accountAddress = args.accountAddress
            ).onSuccess { validatorWithStakes ->
                _state.update { it.copy(validatorWithStakes = validatorWithStakes) }
            }.onFailure { error ->
                Timber.w(error)
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val resourceAddress: String,
        val accountAddress: String?,
        val validatorWithStakes: ValidatorWithStakes?,
        val uiMessage: UiMessage? = null
    ) : UiState
}
