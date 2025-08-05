package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourcesOfTypeUseCase
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.callSafely
import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LedgerDevicesViewModel @Inject constructor(
    getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    private val sargonOsManager: SargonOsManager,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<LedgerDevicesViewModel.State>(),
    OneOffEventHandler<LedgerDevicesViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        @Suppress("OPT_IN_USAGE")
        getFactorSourcesOfTypeUseCase<FactorSource.Ledger>()
            .mapLatest { ledgerFactorSources ->
                resetLedgerFactorSourceList()

                ledgerFactorSources.map { ledgerFactorSource ->
                    sargonOsManager.callSafely(dispatcher = defaultDispatcher) {
                        entitiesLinkedToFactorSource(
                            factorSource = FactorSource.Ledger(ledgerFactorSource.value),
                            profileToCheck = ProfileToCheck.Current
                        )
                    }.onSuccess { entitiesLinkedToLedgerDeviceFactorSource ->
                        val factorSourceCard = ledgerFactorSource.value.toFactorSourceCard(
                            messages = persistentListOf(),
                            accounts = entitiesLinkedToLedgerDeviceFactorSource.accounts.toPersistentList(),
                            personas = entitiesLinkedToLedgerDeviceFactorSource.personas.toPersistentList(),
                            hasHiddenEntities = entitiesLinkedToLedgerDeviceFactorSource.hiddenAccounts.isNotEmpty() ||
                                entitiesLinkedToLedgerDeviceFactorSource.hiddenPersonas.isNotEmpty()
                        )
                        // avoid duplication when a factor source is updated in the Factor Source Details screen
                        val updatedLedgerFactorSources = _state.value.ledgerFactorSources
                            .filterNot { it.id == factorSourceCard.id }
                            .toMutableList()
                        updatedLedgerFactorSources.add(factorSourceCard)
                        _state.update { state ->
                            state.copy(ledgerFactorSources = updatedLedgerFactorSources.toPersistentList())
                        }
                    }.onFailure { error ->
                        Timber.e("Failed to find linked entities: $error")
                    }
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
    }

    private fun resetLedgerFactorSourceList() {
        _state.update { state -> state.copy(ledgerFactorSources = persistentListOf()) }
    }

    private fun LedgerHardwareWalletFactorSource.toFactorSourceCard(
        includeDescription: Boolean = false,
        messages: PersistentList<FactorSourceStatusMessage> = persistentListOf(),
        accounts: PersistentList<Account> = persistentListOf(),
        personas: PersistentList<Persona> = persistentListOf(),
        hasHiddenEntities: Boolean
    ): FactorSourceCard {
        return FactorSourceCard(
            id = id.asGeneral(),
            name = hint.label,
            includeDescription = includeDescription,
            lastUsedOn = common.lastUsedOn.relativeTimeFormatted(),
            kind = kind,
            messages = messages,
            accounts = accounts,
            personas = personas,
            hasHiddenEntities = hasHiddenEntities,
            supportsBabylon = asGeneral().supportsBabylon,
            supportsOlympia = asGeneral().supportsOlympia,
            isEnabled = true
        )
    }

    fun onLedgerFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToLedgerFactorSourceDetails(factorSourceId))
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(
                AddFactorSourceInput.WithKindPreselected(
                    kind = FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
                    context = AddFactorSourceInput.Context.New
                )
            )
        }
    }

    data class State(
        val ledgerFactorSources: PersistentList<FactorSourceCard> = persistentListOf()
    ) : UiState

    sealed interface Event : OneOffEvent {

        data class NavigateToLedgerFactorSourceDetails(val factorSourceId: FactorSourceId) : Event
    }
}
