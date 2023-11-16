package com.babylon.wallet.android.presentation.status.assets.fungible

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.assets.ObserveResourceUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FungibleAssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeResourceUseCase: ObserveResourceUseCase
) : StateViewModel<FungibleAssetDialogViewModel.State>() {

    private val args = FungibleAssetDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        resourceAddress = args.resourceAddress,
        accountAddress = args.accountAddress,
        resource = null
    )

    init {
        observeResourceUseCase(
            resourceAddress = args.resourceAddress,
            accountAddress = args.accountAddress
        ).filterIsInstance<Resource.FungibleResource>()
            .onEach { resource ->
                _state.update { it.copy(resource = resource) }
            }
            .catch { error ->
                Timber.w(error)
                _state.update { it.copy(uiMessage = UiMessage.ErrorMessage(error)) }
            }
            .launchIn(viewModelScope)
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val resourceAddress: String,
        val accountAddress: String?,
        val resource: Resource.FungibleResource?,
        val uiMessage: UiMessage? = null
    ) : UiState
}
