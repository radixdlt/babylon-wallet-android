package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.changepin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.createpin.CreateArculusPinState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.FactorSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class ChangeArculusPinViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val arculusCardClient: ArculusCardClient,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<ChangeArculusPinViewModel.State>(),
    OneOffEventHandler<ChangeArculusPinViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = ChangeArculusPinArgs(savedStateHandle)

    override fun initialState(): State = State()

    fun onPinChange(value: String) {
        updateCreatePinState { state -> state.copy(pin = value) }
    }

    fun onConfirmPinChange(value: String) {
        updateCreatePinState { state -> state.copy(confirmedPin = value) }
    }

    fun onConfirmClick() {
        updateCreatePinState { state -> state.copy(isConfirmButtonLoading = true) }

        viewModelScope.launch {
            val factorSource = getProfileUseCase().factorSourceById(args.factorSourceId) as? FactorSource.ArculusCard
                ?: error("Arculus factor source not found")

            arculusCardClient.setPin(
                factorSource = factorSource,
                oldPin = args.oldPin,
                newPin = state.value.createPinState.pin
            ).onSuccess {
                updateCreatePinState { state -> state.copy(isConfirmButtonLoading = false) }
                appEventBus.sendEvent(AppEvent.GenericSuccess)
                sendEvent(Event.PinChanged)
            }.onFailure {
                updateCreatePinState { state ->
                    state.copy(
                        isConfirmButtonLoading = false,
                        uiMessage = UiMessage.ErrorMessage(it)
                    )
                }
            }
        }
    }

    fun onDismissUiMessage() {
        updateCreatePinState { state -> state.copy(uiMessage = null) }
    }

    fun updateCreatePinState(update: (CreateArculusPinState) -> CreateArculusPinState) {
        _state.update { state ->
            state.copy(
                createPinState = update(state.createPinState)
            )
        }
    }

    sealed interface Event : OneOffEvent {

        data object PinChanged : Event
    }

    data class State(
        val createPinState: CreateArculusPinState = CreateArculusPinState(),
    ) : UiState
}
