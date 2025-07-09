package com.babylon.wallet.android.presentation.addfactorsource.identify

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.RadixWalletException.LedgerCommunicationException.FailedToGetDeviceId
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.UUIDGenerator
import rdx.works.core.sargon.factorSourceById
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class IdentifyFactorSourceViewModel @Inject constructor(
    addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val ledgerMessenger: LedgerMessenger,
    private val getProfileUseCase: GetProfileUseCase
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

            identifyLedgerFactorSource()

            _state.update { state -> state.copy(isInProgress = false) }
        }
    }

    private suspend fun identifyLedgerFactorSource() {
        ledgerMessenger.sendDeviceInfoRequest(
            interactionId = UUIDGenerator.uuid().toString()
        ).onSuccess { deviceInfoResponse ->
            val existingLedgerFactorSource = getProfileUseCase().factorSourceById(deviceInfoResponse.factorSourceId)

            if (existingLedgerFactorSource == null) {
                sendEvent(
                    Event.LedgerIdentified(
                        factorSourceId = deviceInfoResponse.factorSourceId,
                        model = deviceInfoResponse.model.toProfileLedgerDeviceModel()
                    )
                )
            } else {
                _state.update { state ->
                    state.copy(
                        errorMessage = UiMessage.ErrorMessage(
                            error = RadixWalletException.AddFactorSource.FactorSourceAlreadyInUse
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

    data class State(
        val factorSourceKind: FactorSourceKind,
        val errorMessage: UiMessage.ErrorMessage? = null,
        val isInProgress: Boolean = false
    ) : UiState {

        val isRetryEnabled = !isInProgress
    }

    sealed interface Event : OneOffEvent {

        data class LedgerIdentified(
            val factorSourceId: FactorSourceId.Hash,
            val model: LedgerHardwareWalletModel
        ) : Event
    }
}
