package com.babylon.wallet.android.presentation.settings.preferences.ss.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.preferences.ss.details.SignalingServerDetailsViewModel.State.Mode
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.P2pTransportProfile
import com.radixdlt.sargon.extensions.all
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignalingServerDetailsViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<SignalingServerDetailsViewModel.State>(),
    OneOffEventHandler<SignalingServerDetailsViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SignalingServerDetailsNavArgs(savedStateHandle)

    init {
        if (args.id == null) {
            _state.update { state ->
                state.copy(
                    mode = Mode.Add
                )
            }
        } else {
            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    p2pTransportProfiles().all.firstOrNull { it.id == args.id }
                        ?: error("Server with ${args.id} id not found")
                }.onFailure {
                    onError(it)
                }.onSuccess {
                    _state.update { state ->
                        state.copy(
                            mode = Mode.Edit(
                                p2pTransportProfile = it
                            )
                        )
                    }
                }
            }
        }
    }

    override fun initialState(): State = State()

    fun onChangeAsCurrent() {
        val p2pTransportProfile = state.value.editMode?.p2pTransportProfile ?: return

        viewModelScope.launch {
            sargonOsManager.callSafely(defaultDispatcher) {
                changeCurrentP2pTransportProfile(p2pTransportProfile)
            }.onFailure {
                onError(it)
            }.onSuccess { changed ->
                if (changed) {
                    // TODO update UI to show as current
                } else {
                    onError("Failed to change server")
                }
            }
        }
    }

    fun onDeleteClick() {
        _state.update { state ->
            state.copy(
                showDeleteConfirmation = true
            )
        }
    }

    fun onDeleteConfirmationDismissed(confirmed: Boolean) {
        _state.update { state ->
            state.copy(
                showDeleteConfirmation = false
            )
        }

        if (confirmed) {
            val p2pTransportProfile = state.value.editMode?.p2pTransportProfile ?: return

            viewModelScope.launch {
                sargonOsManager.callSafely(defaultDispatcher) {
                    deleteP2pTransportProfile(p2pTransportProfile)
                }.onFailure {
                    onError(it)
                }.onSuccess { deleted ->
                    if (deleted) {
                        sendEvent(Event.Dismiss)
                    } else {
                        onError("Failed to delete server")
                    }
                }
            }
        }
    }

    fun onDismissErrorMessage() {
        _state.update { state ->
            state.copy(
                errorMessage = null
            )
        }
    }

    fun onSaveClick() {
        // TODO implement
    }

    private fun onError(message: String) {
        onError(Throwable(message))
    }

    private fun onError(throwable: Throwable) {
        _state.update { state ->
            state.copy(
                errorMessage = UiMessage.ErrorMessage(throwable)
            )
        }
    }

    data class State(
        val mode: Mode? = null,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val showDeleteConfirmation: Boolean = false,
    ) : UiState {

        val editMode: Mode.Edit? = mode as? Mode.Edit

        sealed interface Mode {

            data class Edit(
                val p2pTransportProfile: P2pTransportProfile
            ) : Mode

            object Add : Mode
        }
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event
    }
}
