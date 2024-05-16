package com.babylon.wallet.android.presentation.settings.linkedconnectors.upgrade

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RelinkConnectorsViewModel @Inject constructor(
    private val p2PLinksRepository: P2PLinksRepository
) : StateViewModel<RelinkConnectorsViewModel.UiState>() {

    init {
        viewModelScope.launch {
            _state.update {
                when {
                    p2PLinksRepository.showRelinkConnectorsAfterUpdate() -> {
                        UiState.AppUpdate
                    }
                    p2PLinksRepository.showRelinkConnectorsAfterProfileRestore() -> {
                        UiState.ProfileRestore
                    }
                    else -> {
                        Timber.e("Relink Connectors init error. Must be either from app update or profile restore")
                        UiState.Idle
                    }
                }
            }
        }
    }

    override fun initialState(): UiState {
        return UiState.Idle
    }

    fun acknowledgeMessage() {
        viewModelScope.launch {
            p2PLinksRepository.clearShowRelinkConnectors()
        }
    }

    sealed interface UiState : com.babylon.wallet.android.presentation.common.UiState {

        data object Idle : UiState

        data object AppUpdate : UiState

        data object ProfileRestore : UiState
    }
}
