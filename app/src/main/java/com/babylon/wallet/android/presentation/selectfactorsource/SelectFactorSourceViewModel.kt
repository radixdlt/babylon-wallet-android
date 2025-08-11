package com.babylon.wallet.android.presentation.selectfactorsource

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.factorsources.GetEntitiesLinkedToFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceInput
import com.babylon.wallet.android.presentation.addfactorsource.AddFactorSourceProxy
import com.babylon.wallet.android.presentation.addfactorsource.kind.isSupported
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.common.toUiItem
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import com.radixdlt.sargon.extensions.supportsBabylon
import com.radixdlt.sargon.extensions.supportsOlympia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.lastUsedOn
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject
import kotlin.collections.filterIsInstance

@HiltViewModel
class SelectFactorSourceViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    private val getEntitiesLinkedToFactorSourceUseCase: GetEntitiesLinkedToFactorSourceUseCase,
    private val getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase,
    private val selectFactorSourceIOHandler: SelectFactorSourceIOHandler,
    private val addFactorSourceProxy: AddFactorSourceProxy,
    private val appEventBus: AppEventBus,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : StateViewModel<SelectFactorSourceViewModel.State>(),
    OneOffEventHandler<SelectFactorSourceViewModel.Event> by OneOffEventHandlerImpl() {

    private val input = selectFactorSourceIOHandler.getInput() as SelectFactorSourceInput.WithContext

    private val refreshFlow = MutableSharedFlow<Unit>()
    private var observeDataJob: Job? = null

    private val data: StateFlow<Data> = combine(
        refreshFlow.onStart { emit(Unit) },
        getProfileUseCase.flow.map { it.factorSources }
    ) { _, allFactorSources ->
        allFactorSources
    }.map { allFactorSources ->
        val eligibleFactorSources = allFactorSources.filter { factorSource ->
            factorSource.kind in input.context.supportedKinds
        }.filter { factorSource ->
            when (val context = input.context) {
                SelectFactorSourceInput.Context.CreateAccount,
                SelectFactorSourceInput.Context.CreatePersona -> factorSource.supportsBabylon

                is SelectFactorSourceInput.Context.AccountRecovery -> if (context.isOlympia) {
                    factorSource.supportsOlympia
                } else {
                    factorSource.supportsBabylon
                }
            }
        }

        Data(
            factorSources = eligibleFactorSources.groupBy { it.kind },
            entitiesLinkedToFactorSourceById = eligibleFactorSources.mapNotNull { factorSource ->
                val entities = getEntitiesLinkedToFactorSourceUseCase(factorSource) ?: return@mapNotNull null
                factorSource.id to entities
            }.toMap(),
            statusMessagesByFactorSourceId = getFactorSourceIntegrityStatusMessagesUseCase.forDeviceFactorSources(
                deviceFactorSources = eligibleFactorSources.filterIsInstance<FactorSource.Device>(),
                includeNoIssuesMessage = true,
                checkIntegrityOnlyIfAnyEntitiesLinked = false
            )
        )
    }.flowOn(defaultDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        initialValue = Data()
    )

    init {
        observeData()
        observeSecurityIssueEvents()
    }

    override fun initialState(): State = State(
        isLoading = true,
        context = input.context
    )

    fun onSelectFactorSource(card: FactorSourceCard) {
        setSelectedFactorSource(card.id)
    }

    fun onAddFactorSourceClick() {
        viewModelScope.launch {
            addFactorSourceProxy.addFactorSource(
                AddFactorSourceInput.SelectKind(
                    kinds = input.context.supportedKinds,
                    context = when (input.context) {
                        is SelectFactorSourceInput.Context.AccountRecovery -> AddFactorSourceInput.Context.Recovery(
                            isOlympia = input.context.isOlympia
                        )

                        SelectFactorSourceInput.Context.CreateAccount,
                        SelectFactorSourceInput.Context.CreatePersona -> AddFactorSourceInput.Context.New
                    }
                )
            )?.value?.let { addedFactorSourceId ->
                setSelectedFactorSource(addedFactorSourceId)
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            selectFactorSourceIOHandler.setOutput(SelectFactorSourceOutput.Init)
            sendEvent(Event.Dismiss)
        }
    }

    fun onContinueClick() {
        val selectedFactorSourceCard = checkNotNull(state.value.selectedFactorSourceCard)

        viewModelScope.launch {
            selectFactorSourceIOHandler.setOutput(SelectFactorSourceOutput.Id(selectedFactorSourceCard.id))

            sendEvent(
                Event.Complete(
                    factorSourceId = selectedFactorSourceCard.id
                )
            )
        }
    }

    private fun observeData(unusableFactorSources: List<FactorSourceId> = emptyList()) {
        observeDataJob?.cancel()
        observeDataJob = viewModelScope.launch {
            data.collect { data ->
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        items = data.factorSources.map { (kind, factorSources) ->
                            listOf(
                                State.UiItem.CategoryHeader(
                                    kind = kind
                                )
                            ).plus(
                                factorSources.sortedByDescending { it.lastUsedOn }.map { factorSource ->
                                    State.UiItem.Factor(
                                        selectable = factorSource.toUiItem(
                                            entitiesLinkedToFactorSourceById = data.entitiesLinkedToFactorSourceById,
                                            statusMessagesByFactorSourceId = data.statusMessagesByFactorSourceId,
                                            unusableFactorSources = unusableFactorSources,
                                            includeNoIssuesMessage = true
                                        )
                                    )
                                }
                            )
                        }.flatten()
                    )
                }
            }
        }
    }

    private fun setSelectedFactorSource(id: FactorSourceId) {
        _state.update { state ->
            state.copy(
                items = state.items.map { item ->
                    when (item) {
                        is State.UiItem.CategoryHeader -> item
                        is State.UiItem.Factor -> item.copy(
                            selectable = item.selectable.copy(
                                selected = item.selectable.data.id == id
                            )
                        )
                    }
                }
            )
        }
    }

    private fun observeSecurityIssueEvents() {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.FixSecurityIssue>().collect {
                when (it) {
                    is AppEvent.FixSecurityIssue.ImportedMnemonic,
                    is AppEvent.FixSecurityIssue.WrittenDownSeedPhrase -> refreshFlow.emit(Unit)

                    is AppEvent.FixSecurityIssue.ImportMnemonic,
                    is AppEvent.FixSecurityIssue.WriteDownSeedPhrase -> null
                }
            }
        }
    }

    private data class Data(
        val factorSources: Map<FactorSourceKind, List<FactorSource>> = emptyMap(),
        val entitiesLinkedToFactorSourceById: Map<FactorSourceId, EntitiesLinkedToFactorSource> = emptyMap(),
        val statusMessagesByFactorSourceId: Map<FactorSourceId, List<FactorSourceStatusMessage>> = emptyMap()
    )

    data class State(
        val isLoading: Boolean,
        val context: SelectFactorSourceInput.Context,
        val items: List<UiItem> = emptyList()
    ) : UiState {

        val selectedFactorSourceCard = items.filterIsInstance<UiItem.Factor>()
            .firstOrNull { it.selectable.selected }?.selectable?.data
        val isButtonEnabled: Boolean = selectedFactorSourceCard != null

        sealed interface UiItem {

            data class CategoryHeader(
                val kind: FactorSourceKind
            ) : UiItem

            data class Factor(
                val selectable: Selectable<FactorSourceCard>
            ) : UiItem
        }
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event

        data class Complete(
            val factorSourceId: FactorSourceId
        ) : Event
    }
}

private val SelectFactorSourceInput.Context.supportedKinds
    get() = when {
        (this is SelectFactorSourceInput.Context.AccountRecovery && isOlympia) -> listOf(
            FactorSourceKind.DEVICE,
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
        )

        else -> listOf(
            FactorSourceKind.DEVICE,
            FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET,
            FactorSourceKind.ARCULUS_CARD
        )
    }.filter { it.isSupported }
