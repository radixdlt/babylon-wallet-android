package com.babylon.wallet.android.presentation.addfactorsource.identify

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIntermediaryParams
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.common.ArculusCardClient
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.name
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class IdentifyFactorSourceViewModel @Inject constructor(
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val ledgerMessenger: LedgerMessenger,
    private val getProfileUseCase: GetProfileUseCase,
    private val arculusCardClient: ArculusCardClient,
) : StateViewModel<IdentifyFactorSourceViewModel.State>(),
    OneOffEventHandler<IdentifyFactorSourceViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind

    init {
        identifyFactorSource()
    }

    override fun initialState(): State = State(
        factorSourceKind = checkNotNull(input.kind)
    )

    fun onRetry() {
        identifyFactorSource()
    }

    fun onMessageShown() {
        _state.update { state -> state.copy(errorMessage = null) }
    }

    private fun identifyFactorSource() {
        viewModelScope.launch {
            _state.update { state -> state.copy(isInProgress = true) }

            when (input.kind) {
                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> identifyLedgerFactorSource()
                FactorSourceKind.ARCULUS_CARD -> identifyArculusFactorSource()
                FactorSourceKind.DEVICE,
                FactorSourceKind.OFF_DEVICE_MNEMONIC,
                FactorSourceKind.PASSWORD -> error("Shouldn't be here")
            }

            _state.update { state -> state.copy(isInProgress = false) }
        }
    }

    private suspend fun identifyLedgerFactorSource() {
        ledgerMessenger.sendDeviceInfoRequest(
            interactionId = UUIDGenerator.uuid().toString()
        ).onSuccess { deviceInfoResponse ->
            val existingLedgerFactorSource = getProfileUseCase().factorSourceById(deviceInfoResponse.factorSourceId)

            if (existingLedgerFactorSource == null) {
                addFactorSourceIOHandler.setIntermediaryParams(
                    AddFactorSourceIntermediaryParams.Ledger(
                        factorSourceId = deviceInfoResponse.factorSourceId,
                        model = deviceInfoResponse.model.toProfileLedgerDeviceModel()
                    )
                )
                sendEvent(Event.LedgerIdentified)
            } else {
                _state.update { state ->
                    state.copy(
                        errorMessage = UiMessage.ErrorMessage(
                            error = RadixWalletException.FactorSource.FactorSourceAlreadyInUse(
                                factorSourceName = existingLedgerFactorSource.name
                            )
                        )
                    )
                }
            }
        }.onFailure { error ->
            _state.update { state ->
                state.copy(
                    errorMessage = if (error is FailedToGetDeviceId) {
                        null
                    } else {
                        UiMessage.ErrorMessage(error)
                    },
                    isInProgress = false
                )
            }
        }
    }

    private suspend fun identifyArculusFactorSource() {
        arculusCardClient.validateMinFirmwareVersion().onSuccess {
            sendEvent(Event.ArculusIdentified)
        }.onFailure {
            _state.update { state -> state.copy(errorMessage = UiMessage.ErrorMessage(it)) }
        }
    }

    data class State(
        val factorSourceKind: FactorSourceKind,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val isInProgress: Boolean = false
    ) : UiState {

        val isRetryEnabled = !isInProgress
    }

    sealed interface Event : OneOffEvent {

        data object LedgerIdentified : Event

        data object ArculusIdentified : Event
    }
}
