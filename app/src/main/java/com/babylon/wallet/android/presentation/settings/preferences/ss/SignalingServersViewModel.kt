package com.babylon.wallet.android.presentation.settings.preferences.ss

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SignalingServersViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SignalingServersViewModel.State>() {

    override fun initialState(): State = State()

    fun onAddClick() {

    }

    fun onItemClick(item: State.UiItem) {

    }

    fun onDeleteItemClick(item: State.UiItem) {

    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        _state.update { state ->
            state.copy(
                itemToDelete = null
            )
        }
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val itemToDelete: P2pTransportProfile? = null
    ) : UiState {

        data class UiItem(
            val server: P2pTransportProfile,
            val selected: Boolean
        ) {

            val url = server.signalingServer.toString()
        }
    }
}