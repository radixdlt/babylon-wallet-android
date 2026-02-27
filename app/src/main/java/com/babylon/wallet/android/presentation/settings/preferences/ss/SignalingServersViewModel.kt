package com.babylon.wallet.android.presentation.settings.preferences.ss

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.SavedP2pTransportProfiles
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.changeCurrent
import com.radixdlt.sargon.extensions.remove
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignalingServersViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SignalingServersViewModel.State>() {

    init {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                val p2pTransportProfiles = profile().appPreferences.p2pTransportProfiles

                _state.update { state ->
                    state.copy(
                        items = p2pTransportProfiles.toUiItems()
                    )
                }
            }
        }
    }

    override fun initialState(): State = State()

    fun onAddClick() {
        // TODO navigate to add
    }

    fun onItemClick(item: State.UiItem) {
        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                val updatedServers = profile().appPreferences.p2pTransportProfiles
                updatedServers.changeCurrent(item.server)

                // TODO update profile

                _state.update { state ->
                    state.copy(
                        items = updatedServers.toUiItems()
                    )
                }
            }
        }
    }

    fun onDeleteItemClick(item: State.UiItem) {
        _state.update { state ->
            state.copy(
                itemToDelete = item.server
            )
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        val itemToDelete = _state.value.itemToDelete ?: return

        _state.update { state ->
            state.copy(
                itemToDelete = null
            )
        }

        if (confirmed) {
            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    val updatedServers = profile().appPreferences.p2pTransportProfiles
                    updatedServers.remove(itemToDelete)

                    // TODO update profile

                    _state.update { state ->
                        state.copy(
                            items = updatedServers.toUiItems()
                        )
                    }
                }
            }
        }
    }

    private fun SavedP2pTransportProfiles.toUiItems(): List<State.UiItem> = all.map {
        State.UiItem(
            server = it,
            selected = it == current
        )
    }

    data class State(
        val items: List<UiItem> = emptyList(),
        val itemToDelete: P2pTransportProfile? = null
    ) : UiState {

        data class UiItem(
            val server: P2pTransportProfile,
            val selected: Boolean
        ) {

            val url = server.signalingServer
        }
    }
}
