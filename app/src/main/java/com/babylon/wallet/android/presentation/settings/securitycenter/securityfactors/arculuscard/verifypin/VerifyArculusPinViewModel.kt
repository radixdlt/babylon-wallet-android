package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.verifypin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.babylon.wallet.android.utils.Constants.ARCULUS_PIN_LENGTH
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class VerifyArculusPinViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val arculusCardClient: ArculusCardClient,
    savedStateHandle: SavedStateHandle
) : StateViewModel<VerifyArculusPinViewModel.State>(),
    OneOffEventHandler<VerifyArculusPinViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = VerifyArculusPinArgs(savedStateHandle)

    override fun initialState(): State = State()

    fun onPinChange(value: String) {
        _state.update { state -> state.copy(pin = value) }
    }

    fun onContinueClick() {
        _state.update { state -> state.copy(isContinueLoading = true) }

        viewModelScope.launch {
            val factorSource = getProfileUseCase().factorSourceById(args.factorSourceId) as? FactorSource.ArculusCard
                ?: error("Arculus factor source not found")

            arculusCardClient.verifyPin(
                factorSource = factorSource,
                pin = state.value.pin
            ).onSuccess {
                _state.update { state -> state.copy(isContinueLoading = false) }
                sendEvent(
                    Event.Complete(
                        factorSourceId = args.factorSourceId,
                        pin = state.value.pin
                    )
                )
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        isContinueLoading = false,
                        pin = if (it is CommonException.NfcSessionCancelled) {
                            state.pin
                        } else {
                            ""
                        },
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

        data class Complete(
            val factorSourceId: FactorSourceId,
            val pin: String
        ) : Event
    }

    data class State(
        val pin: String = "",
        val isContinueLoading: Boolean = false,
        val errorMessage: UiMessage.ErrorMessage? = null
    ) : UiState {

        val isContinueEnabled = pin.length == ARCULUS_PIN_LENGTH && !isContinueLoading
    }
}
