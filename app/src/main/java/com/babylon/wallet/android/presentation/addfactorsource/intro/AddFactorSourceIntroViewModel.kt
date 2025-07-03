package com.babylon.wallet.android.presentation.addfactorsource.intro

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceIOHandler
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFactorSourceIntroViewModel @Inject constructor(
    private val addFactorSourceIOHandler: AddFactorSourceIOHandler,
    private val p2pLinksRepository: P2PLinksRepository,
    private val appEventBus: AppEventBus
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
            // TODO link connector flow
            return
        }

        val event = appEventBus.events
            .filterIsInstance<AppEvent.AccessFactorSources.SelectLedgerOutcome>()
            .first()

        when (event) {
            is AppEvent.AccessFactorSources.SelectLedgerOutcome.Rejected -> null
            is AppEvent.AccessFactorSources.SelectLedgerOutcome.Selected -> {
                addFactorSourceIOHandler.setOutput(AddFactorSourceOutput.Id(event.ledgerFactorSource.value.id.asGeneral()))
                sendEvent(Event.Dismiss)
            }
        }
    }

    data class State(
        val factorSourceKind: FactorSourceKind
    ) : UiState

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event

        data object AddDeviceFactorSource : Event

        data object AddLedgerFactorSource : Event
    }
}
