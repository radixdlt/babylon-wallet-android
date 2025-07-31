package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.choosefactor

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.FactorSourceKindsByCategory
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.usecases.factorsources.GetEntitiesLinkedToFactorSourceUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceIntegrityStatusMessagesUseCase
import com.babylon.wallet.android.domain.usecases.factorsources.GetFactorSourceKindsByCategoryUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.common.toUiItems
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceCard
import com.babylon.wallet.android.presentation.ui.model.factors.FactorSourceStatusMessage
import com.babylon.wallet.android.presentation.ui.model.factors.SecurityFactorTypeUiItem
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.EntitiesLinkedToFactorSource
import com.radixdlt.sargon.FactorSource
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.id
import com.radixdlt.sargon.extensions.kind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChooseFactorSourceViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getFactorSourceKindsByCategoryUseCase: GetFactorSourceKindsByCategoryUseCase,
    private val getEntitiesLinkedToFactorSourceUseCase: GetEntitiesLinkedToFactorSourceUseCase,
    private val getFactorSourceIntegrityStatusMessagesUseCase: GetFactorSourceIntegrityStatusMessagesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : StateViewModel<ChooseFactorSourceViewModel.State>(), OneOffEventHandler<ChooseFactorSourceViewModel.Event> by OneOffEventHandlerImpl() {

    private val data = getProfileUseCase.flow.map { it.factorSources }
        .map { allFactorSources ->
            Data(
                kindsByCategories = getFactorSourceKindsByCategoryUseCase(),
                allFactorSources = allFactorSources.groupBy { it.kind },
                entitiesLinkedToFactorSourceById = allFactorSources.mapNotNull { factorSource ->
                    val entities = getEntitiesLinkedToFactorSourceUseCase(factorSource) ?: return@mapNotNull null
                    factorSource.id to entities
                }.toMap(),
                statusMessagesByFactorSourceId = getFactorSourceIntegrityStatusMessagesUseCase.forDeviceFactorSources(
                    deviceFactorSources = allFactorSources.filterIsInstance<FactorSource.Device>(),
                    includeNoIssuesMessage = false,
                    checkIntegrityOnlyIfAnyEntitiesLinked = false
                )
            )
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
            initialValue = Data()
        )

    private var observeDataJob: Job? = null

    override fun initialState(): State = State()

    fun initData(
        unusableFactorSourceKinds: List<FactorSourceKind>,
        alreadySelectedFactorSources: List<FactorSourceId>,
        unusableFactorSources: List<FactorSourceId>
    ) {
        observeDataJob?.cancel()
        observeDataJob = viewModelScope.launch {
            data.collect { data ->
                _state.update { state ->
                    state.copy(
                        pages = persistentListOf(
                            State.Page.SelectFactorSourceType(
                                items = data.kindsByCategories.toTypeUiItems(unusableFactorSourceKinds)
                                    .toPersistentList()
                            )
                        ).plus(
                            data.kindsByCategories.map { it.kinds }
                                .flatten()
                                .map { kind ->
                                    State.Page.SelectFactorSource(
                                        kind = kind,
                                        items = data.allFactorSources.getOrDefault(kind, emptyList())
                                            .toUiItems(
                                                entitiesLinkedToFactorSourceById = data.entitiesLinkedToFactorSourceById,
                                                statusMessagesByFactorSourceId = data.statusMessagesByFactorSourceId,
                                                alreadySelectedFactorSources = alreadySelectedFactorSources,
                                                unusableFactorSources = unusableFactorSources,
                                                includeNoIssuesMessage = false
                                            )
                                            .toPersistentList()
                                    )
                                }
                        ),
                        currentPagePosition = 0
                    )
                }
            }
        }
    }

    fun onSecurityFactorTypeClick(item: SecurityFactorTypeUiItem.Item) {
        _state.update { state ->
            val nextPagePosition = state.pages.indexOfFirst { it is State.Page.SelectFactorSource && it.kind == item.factorSourceKind }
                .takeIf { it != -1 } ?: return

            state.copy(currentPagePosition = nextPagePosition)
        }
    }

    fun onFactorSourceSelect(factorSourceCard: FactorSourceCard) {
        _state.update { state ->
            val currentPage = state.currentSelectFactorSourcePage ?: return@update state

            state.copy(
                pages = state.pages.mapWhen(
                    predicate = { it == currentPage },
                    mutation = {
                        currentPage.copy(
                            items = currentPage.items.mapWhen(
                                predicate = { item -> item.data.isEnabled },
                                mutation = { item ->
                                    item.copy(selected = item.data == factorSourceCard)
                                }
                            ).toPersistentList()
                        )
                    }
                ).toPersistentList()
            )
        }
    }

    fun onSelectedFactorSourceConfirm() = viewModelScope.launch {
        val selectedFactorSource = state.value.newlySelectedFactorSource ?: return@launch

        sendEvent(Event.SelectedFactorSourceConfirm(selectedFactorSource.data))
        _state.update { state ->
            state.copy(
                currentPagePosition = state.selectTypePagePosition
            )
        }
    }

    fun onSheetBackClick() = viewModelScope.launch {
        _state.update { state ->
            if (state.currentPagePosition == state.selectTypePagePosition) {
                sendEvent(Event.DismissSheet)
            }
            state.copy(currentPagePosition = state.selectTypePagePosition)
        }
    }

    fun onAddFactorSourceClick(factorSourceKind: FactorSourceKind) {
        Timber.d("onAddFactorSourceClick: $factorSourceKind")
    }

    fun onSheetCloseClick() = viewModelScope.launch {
        _state.update { state ->
            state.copy(currentPagePosition = state.selectTypePagePosition)
        }
        sendEvent(Event.DismissSheet)
    }

    private fun List<FactorSourceKindsByCategory>.toTypeUiItems(
        unusableFactorSourceKinds: List<FactorSourceKind>
    ): List<SecurityFactorTypeUiItem> = map { (category, kinds) ->
        val header = SecurityFactorTypeUiItem.Header(category)
        val items = kinds.map { kind ->
            val isEnabled = kind !in unusableFactorSourceKinds

            SecurityFactorTypeUiItem.Item(
                factorSourceKind = kind,
                messages = if (!isEnabled) {
                    persistentListOf(FactorSourceStatusMessage.CannotBeUsedHere)
                } else {
                    persistentListOf()
                },
                isEnabled = isEnabled
            )
        }

        listOf(header) + items
    }.flatten()

    private data class Data(
        val kindsByCategories: List<FactorSourceKindsByCategory> = emptyList(),
        val allFactorSources: Map<FactorSourceKind, List<FactorSource>> = emptyMap(),
        val entitiesLinkedToFactorSourceById: Map<FactorSourceId, EntitiesLinkedToFactorSource> = emptyMap(),
        val statusMessagesByFactorSourceId: Map<FactorSourceId, List<FactorSourceStatusMessage>> = emptyMap()
    )

    data class State(
        val pages: PersistentList<Page> = persistentListOf(Page.SelectFactorSourceType()),
        val currentPagePosition: Int = 0
    ) : UiState {

        val selectTypePagePosition = pages.indexOfFirst { it is Page.SelectFactorSourceType }
        val currentSelectFactorSourcePage = pages.getOrNull(currentPagePosition) as? Page.SelectFactorSource
        val newlySelectedFactorSource = currentSelectFactorSourcePage?.items?.find { it.selected && it.data.isEnabled }
        val isButtonEnabled = newlySelectedFactorSource != null

        sealed interface Page {

            data class SelectFactorSourceType(
                val items: PersistentList<SecurityFactorTypeUiItem> = persistentListOf()
            ) : Page

            data class SelectFactorSource(
                val kind: FactorSourceKind,
                val items: PersistentList<Selectable<FactorSourceCard>>
            ) : Page
        }
    }

    sealed interface Event : OneOffEvent {

        data object DismissSheet : Event

        data class SelectedFactorSourceConfirm(val factorSourceCard: FactorSourceCard) : Event
    }
}
