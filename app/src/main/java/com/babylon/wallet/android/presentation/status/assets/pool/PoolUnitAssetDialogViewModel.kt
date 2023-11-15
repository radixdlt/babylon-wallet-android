package com.babylon.wallet.android.presentation.status.assets.pool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.usecases.assets.GetPoolUnitDetailsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PoolUnitAssetDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPoolUnitDetailsUseCase: GetPoolUnitDetailsUseCase
) : StateViewModel<PoolUnitAssetDialogViewModel.State>() {

    private val args = PoolUnitAssetDialogArgs(savedStateHandle)
    override fun initialState(): State = State(
        resourceAddress = args.resourceAddress,
        accountAddress = args.accountAddress,
        poolUnit = null
    )

    init {
        viewModelScope.launch {
            getPoolUnitDetailsUseCase(
                resourceAddress = args.resourceAddress,
                accountAddress = args.accountAddress
            ).onSuccess { poolUnit ->
                _state.update { it.copy(poolUnit = poolUnit) }
            }.onFailure {
                Timber.w(it)
            }
        }
    }

    data class State(
        val resourceAddress: String,
        val accountAddress: String?,
        val poolUnit: PoolUnit?
    ) : UiState
}
