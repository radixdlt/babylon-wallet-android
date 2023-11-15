package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.assets.GetNFTDetailsUseCase
import com.babylon.wallet.android.domain.usecases.assets.ObserveResourceUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NonFungibleAssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeResourceUseCase: ObserveResourceUseCase,
    getNFTDetailsUseCase: GetNFTDetailsUseCase
): StateViewModel<NonFungibleAssetDialogViewModel.State>() {

    private val args = NonFungibleAssetDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        resourceAddress = args.resourceAddress,
        localId = args.localId
    )

    init {
        if (args.localId != null) {
            viewModelScope.launch {
                getNFTDetailsUseCase(
                    resourceAddress = _state.value.resourceAddress,
                    localId = args.localId
                ).onSuccess { item ->
                    _state.update { it.copy(item = item) }
                }
            }
        }

        observeResourceUseCase(resourceAddress = args.resourceAddress)
            .filterIsInstance<Resource.NonFungibleResource>()
            .onEach { resource ->
                _state.update { it.copy(resource = resource) }
            }
            .launchIn(viewModelScope)
    }

    data class State(
        val resourceAddress: String,
        val localId: String?,
        val resource: Resource.NonFungibleResource? = null,
        val item: Resource.NonFungibleResource.Item? = null
    ): UiState
}
