package com.babylon.wallet.android.presentation.addfactorsource.intro

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.FactorSourceKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFactorSourceIntroViewModel @Inject constructor(
    addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val p2pLinksRepository: P2PLinksRepository,
    private val ledgerMessenger: LedgerMessenger
) : StateViewModel<AddFactorSourceIntroViewModel.State>(),
    OneOffEventHandler<AddFactorSourceIntroViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = addFactorSourceIOHandler.getInput() as AddFactorSourceInput.WithKind

    override fun initialState(): State = State(
        factorSourceKind = checkNotNull(input.kind)
    )

    fun onContinueClick() {
        val factorSourceKind = state.value.factorSourceKind

        viewModelScope.launch {
            when (factorSourceKind) {
                FactorSourceKind.DEVICE -> sendEvent(Event.AddDeviceFactorSource)
                FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> addLedgerFactorSource()
                FactorSourceKind.OFF_DEVICE_MNEMONIC,
                FactorSourceKind.ARCULUS_CARD,
                FactorSourceKind.PASSWORD -> sendEvent(Event.Dismiss)
            }
        }
    }

    private suspend fun addLedgerFactorSource() {
        val hasAtLeastOneLinkedConnector = p2pLinksRepository.getP2PLinks()
            .asList()
            .isNotEmpty()

        if (hasAtLeastOneLinkedConnector) {
            sendEvent(Event.AddLedgerFactorSource)
        } else {
            sendEvent(Event.AddLinkConnector)
            ledgerMessenger.isAnyLinkedConnectorConnected
                .dropWhile { isConnected ->
                    if (isConnected) {
                        sendEvent(Event.AddLedgerFactorSource)
                    }
                    !isConnected
                }
                .launchIn(viewModelScope)
        }
    }

    data class State(
        val factorSourceKind: FactorSourceKind
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event

        data object AddDeviceFactorSource : Event

        data object AddLinkConnector : Event

        data object AddLedgerFactorSource : Event

        data object AddArculusFactorSource : Event
    }
}
