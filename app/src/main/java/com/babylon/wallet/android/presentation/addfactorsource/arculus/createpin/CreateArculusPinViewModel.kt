package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIntermediaryParams
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.callSafely
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateArculusPinViewModel @Inject constructor(
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val sargonOsManager: SargonOsManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<CreateArculusPinViewModel.State>(),
    OneOffEventHandler<CreateArculusPinViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    fun onPinChange(value: String) {
        _state.update { state -> state.copy(pin = value) }
    }

    fun onConfirmPinChange(value: String) {
        _state.update { state -> state.copy(confirmedPin = value) }
    }

    fun onCreateClick() {
        val params = addFactorSourceIOHandler.getIntermediaryParams()
            as? AddFactorSourceIntermediaryParams.Mnemonic ?: error("Mnemonic is required")

        _state.update { state -> state.copy(isCreateLoading = true) }

        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                arculusCardConfigureWithMnemonic(
                    mnemonic = params.value.mnemonic,
                    pin = state.value.pin
                ).asGeneral()
            }.onSuccess {
                _state.update { state -> state.copy(isCreateLoading = false) }
                sendEvent(Event.PinCreated)
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        isCreateLoading = false,
                        errorMessage = UiMessage.ErrorMessage(it)
                    )
                }
            }
        }
    }

    fun onDismissMessage() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    sealed interface Event : OneOffEvent {

        data object PinCreated : Event
    }

    data class State(
        val pin: String = "",
        val confirmedPin: String = "",
        val isCreateLoading: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val showPinsNotMatchingError = pin.length == PIN_LENGTH &&
            confirmedPin.length == PIN_LENGTH &&
            !pin.equals(confirmedPin, true)
        val isCreateEnabled = !showPinsNotMatchingError
        val isConfirmedPinEnabled = pin.length == PIN_LENGTH

        companion object {

            const val PIN_LENGTH = 6
        }
    }
}
