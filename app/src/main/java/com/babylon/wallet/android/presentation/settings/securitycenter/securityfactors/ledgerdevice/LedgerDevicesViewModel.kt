package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.LedgerMessenger
import com.babylon.wallet.android.data.repository.p2plink.P2PLinksRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourcesOfTypeUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.relativeTimeFormatted
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.LedgerHardwareWalletFactorSource
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.ProfileToCheck
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedgerDevicesViewModel @Inject constructor(
    getFactorSourcesOfTypeUseCase: GetFactorSourcesOfTypeUseCase,
    private val sargonOsManager: SargonOsManager,
    private val ledgerMessenger: LedgerMessenger,
    private val p2PLinksRepository: P2PLinksRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<LedgerDevicesViewModel.State>(),
    OneOffEventHandler<LedgerDevicesViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        getFactorSourcesOfTypeUseCase<FactorSource.Ledger>()
            .map { ledgerFactorSource ->
                val entitiesLinkedToDeviceFactorSource = sargonOsManager.sargonOs.entitiesLinkedToFactorSource(
                    factorSource = FactorSource.Ledger(ledgerFactorSource.value),
                    profileToCheck = ProfileToCheck.Current
                )

                val factorSourceCard = ledgerFactorSource.value.toFactorSourceCard(
                    messages = persistentListOf(),
                    accounts = entitiesLinkedToDeviceFactorSource.accounts.toPersistentList(),
                    personas = entitiesLinkedToDeviceFactorSource.personas.toPersistentList(),
                    hasHiddenEntities = entitiesLinkedToDeviceFactorSource.hiddenAccounts.isNotEmpty() ||
                        entitiesLinkedToDeviceFactorSource.hiddenPersonas.isNotEmpty()
                )

                _state.update { state ->
                    state.copy(ledgerFactorSources = state.ledgerFactorSources.add(factorSourceCard))
                }
            }
            .flowOn(defaultDispatcher)
            .launchIn(viewModelScope)
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
            hasHiddenEntities = hasHiddenEntities
        )
    }

    fun onLedgerFactorSourceClick(factorSourceId: FactorSourceId) {
        viewModelScope.launch {
            sendEvent(Event.NavigateToLedgerFactorSourceDetails(factorSourceId))
        }
    }

    fun onAddLedgerDeviceClick() {
        viewModelScope.launch {
            val hasAtLeastOneLinkedConnector = p2PLinksRepository.getP2PLinks()
                .asList()
                .isNotEmpty()

            if (hasAtLeastOneLinkedConnector) {
                _state.update {
                    it.copy(showContent = State.ShowContent.AddLedger)
                }
            } else {
                _state.update {
                    it.copy(
                        showLinkConnectorPromptState = ShowLinkConnectorPromptState.Show(
                            source = ShowLinkConnectorPromptState.Source.UseLedger
                        )
                    )
                }
            }
        }
    }

    fun dismissConnectorPrompt(linkConnector: Boolean) {
        _state.update {
            it.copy(
                showContent = if (linkConnector) {
                    State.ShowContent.LinkNewConnector
                } else {
                    it.showContent
                },
                showLinkConnectorPromptState = ShowLinkConnectorPromptState.None
            )
        }
    }

    fun disableAddLedgerButtonUntilConnectionIsEstablished() {
        _state.update {
            it.copy(showContent = State.ShowContent.Details)
        }
        ledgerMessenger.isAnyLinkedConnectorConnected
            .dropWhile { isConnected ->
                _state.update { state ->
                    state.copy(isNewLinkedConnectorConnected = isConnected)
                }
                isConnected.not() // continue while isConnected is not true
            }
            .launchIn(viewModelScope)
    }

    fun onCloseClick() {
        _state.update {
            it.copy(showContent = State.ShowContent.Details)
        }
    }

    fun onLinkConnectorClick() {
        _state.update {
            it.copy(showContent = State.ShowContent.AddLinkConnector)
        }
    }

    fun onNewConnectorCloseClick() {
        _state.update {
            it.copy(showContent = State.ShowContent.Details)
        }
    }

    data class State(
        val showContent: ShowContent = ShowContent.Details,
        val ledgerFactorSources: PersistentList<FactorSourceCard> = persistentListOf(),
        val showLinkConnectorPromptState: ShowLinkConnectorPromptState = ShowLinkConnectorPromptState.None,
        val isNewLinkedConnectorConnected: Boolean = true
    ) : UiState {

        sealed interface ShowContent {
            data object Details : ShowContent
            data object AddLedger : ShowContent
            data object LinkNewConnector : ShowContent
            data object AddLinkConnector : ShowContent
        }
    }

    sealed interface Event : OneOffEvent {

        data class NavigateToLedgerFactorSourceDetails(val factorSourceId: FactorSourceId) : Event
    }
}
