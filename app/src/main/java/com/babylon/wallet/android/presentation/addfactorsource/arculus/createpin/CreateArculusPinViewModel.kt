package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.factors.MnemonicBuilderClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.configurepin.ConfigureArculusPinState
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class CreateArculusPinViewModel @Inject constructor(
    private val mnemonicBuilderClient: MnemonicBuilderClient,
    private val arculusCardClient: ArculusCardClient,
    private val getProfileUseCase: GetProfileUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<CreateArculusPinViewModel.State>(),
    OneOffEventHandler<CreateArculusPinViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = CreateArculusPinArgs(savedStateHandle)

    override fun initialState(): State = State()

    fun onPinChange(value: String) {
        updateCreatePinState { state -> state.copy(pin = value) }
    }

    fun onConfirmPinChange(value: String) {
        updateCreatePinState { state -> state.copy(confirmedPin = value) }
    }

    fun onCreateClick() {
        updateCreatePinState { state -> state.copy(isConfirmButtonLoading = true) }

        viewModelScope.launch {
            when (args.context) {
                CreateArculusPinContext.New -> arculusCardClient.configureCardWithMnemonic(
                    mnemonic = mnemonicBuilderClient.getMnemonicWithPassphrase().mnemonic,
                    pin = state.value.createPinState.pin
                )

                CreateArculusPinContext.Restore -> {
                    val factorSourceId = mnemonicBuilderClient.getFactorSourceId(FactorSourceKind.ARCULUS_CARD)
                    val factorSource = getProfileUseCase().factorSourceById(factorSourceId) as? FactorSource.ArculusCard
                        ?: error("Arculus factor source not found")

                    arculusCardClient.restoreCardPin(
                        factorSource = factorSource,
                        mnemonic = mnemonicBuilderClient.getMnemonicWithPassphrase().mnemonic,
                        pin = state.value.createPinState.pin
                    )
                }
            }.onSuccess {
                updateCreatePinState { state -> state.copy(isConfirmButtonLoading = false) }
                sendEvent(Event.PinCreated(args.context))
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

    fun updateCreatePinState(update: (ConfigureArculusPinState) -> ConfigureArculusPinState) {
        _state.update { state ->
            state.copy(
                createPinState = update(state.createPinState)
            )
        }
    }

    sealed interface Event : OneOffEvent {

        data class PinCreated(
            val context: CreateArculusPinContext
        ) : Event
    }

    data class State(
        val createPinState: ConfigureArculusPinState = ConfigureArculusPinState()
    ) : UiState
}
