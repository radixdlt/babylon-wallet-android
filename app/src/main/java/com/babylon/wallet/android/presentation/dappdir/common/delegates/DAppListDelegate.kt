package com.babylon.wallet.android.presentation.dappdir.common.delegates

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DAppDirectory
import com.babylon.wallet.android.domain.usecases.GetDAppDirectoryUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.dappdir.common.delegates.DAppListDelegate.ViewActions
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppCategoryType
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppFilters
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.Category
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import com.radixdlt.sargon.AccountAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

class DAppListDelegate @Inject constructor(
    private val getDAppDirectoryUseCase: GetDAppDirectoryUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModelDelegate<DAppListState>(), ViewActions {

    val directoryState: MutableStateFlow<DAppDirectory?> = MutableStateFlow(null)
    val dAppDataState = MutableStateFlow<Map<AccountAddress, DAppWithDetails.Details>>(emptyMap())

    private val _filtersState: MutableStateFlow<DAppFilters> = MutableStateFlow(DAppFilters())
    private val filtersState: Flow<DAppFilters> = _filtersState.onEach {
        _state.update { state -> state.copy(filters = it) }
    }

    private var loadDAppsJob: Job? = null

    override fun onSearchTermUpdated(term: String) {
        _filtersState.update { it.copy(searchTerm = term) }
    }

    override fun onFilterTagAdded(tag: String) {
        _filtersState.update { it.copy(selectedTags = it.selectedTags + tag) }
    }

    override fun onFilterTagRemoved(tag: String) {
        _filtersState.update { it.copy(selectedTags = it.selectedTags - tag) }
    }

    override fun onAllFilterTagsRemoved() {
        _filtersState.update { it.copy(selectedTags = emptySet()) }
    }

    fun initialize(
        scope: CoroutineScope,
        state: MutableStateFlow<DAppListState>,
        dAppsWithDetailsState: Flow<List<DAppWithDetails>>
    ) {
        super.invoke(scope, state)
        loadDAppDirectory()
        observeDAppsData()
        observeStateChanges(dAppsWithDetailsState)
    }

    fun loadDAppDirectory() {
        loadDAppsJob?.cancel()
        loadDAppsJob = getDAppDirectoryUseCase(
            isRefreshing = _state.value.isRefreshing
        ).onEach { result ->
            result.onFailure { error ->
                onDAppsLoadingError(error)
            }.onSuccess { directory ->
                directoryState.update { directory }
                onDAppsLoaded(directory.all.map { it.dAppDefinitionAddress })
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun onDAppsLoaded(dAppDefinitionAddresses: List<AccountAddress>) {
        dAppDataState.update { data ->
            data + dAppDefinitionAddresses.toSet().map { dAppDefinitionAddress ->
                dAppDefinitionAddress to DAppWithDetails.Details.Fetching
            }
        }
    }

    fun onDAppsLoadingError(error: Throwable) {
        _state.update {
            it.copy(
                uiMessage = UiMessage.ErrorMessage(error),
                isLoading = false,
                errorLoading = true
            )
        }
    }

    private fun observeDAppsData() {
        dAppDataState.map { data ->
            data.filter { it.value is DAppWithDetails.Details.Fetching }
                .keys.toSet()
        }.filter { unknownDefinitions ->
            unknownDefinitions.isNotEmpty()
        }.onEach { unknownDefinitions ->
            getDAppsUseCase(
                definitionAddresses = unknownDefinitions,
                needMostRecentData = _state.value.isRefreshing
            ).onFailure { error ->
                dAppDataState.update { data ->
                    data.mapValues {
                        if (it.value is DAppWithDetails.Details.Fetching) {
                            DAppWithDetails.Details.Error
                        } else {
                            it.value
                        }
                    }
                }

                disableRefreshing()
            }.onSuccess { dApps ->
                val dAppDefinitionWithDetails = dApps.associateBy { it.dAppAddress }

                dAppDataState.update { data ->
                    data.mapValues {
                        val dApp = dAppDefinitionWithDetails[it.key]

                        if (dApp != null) {
                            DAppWithDetails.Details.Data(
                                name = dApp.name.orEmpty(),
                                iconUri = dApp.iconUrl,
                                description = dApp.description
                            )
                        } else {
                            DAppWithDetails.Details.Error
                        }
                    }
                }

                // TODO sergiu expose tags from metadata
                _filtersState.update {
                    it.copy(
                        availableTags = directoryState.value?.all?.map { it.tags }?.flatten()?.toSet().orEmpty()
                    )
                }
            }
        }.catch { error ->
            onDAppsLoadingError(error)
        }.launchIn(viewModelScope)
    }

    private fun observeStateChanges(dAppsWithDetailsState: Flow<List<DAppWithDetails>>) {
        viewModelScope.launch {
            combine(
                directoryState,
                dAppsWithDetailsState,
                filtersState
            ) { directories, dAppsWithDetails, filters ->
                val items = dAppsWithDetails.filterItems(filters, directories)
                items to directories
            }.onEach { itemsAndDirectories ->
                val itemsByCategory = itemsAndDirectories.first.groupBy { item ->
                    val directoryDefinition = itemsAndDirectories.second?.findByAddress(item.dAppDefinitionAddress)
                    DAppCategoryType.from(directoryDefinition?.category)
                }

                disableRefreshing()
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        items = itemsByCategory.entries.sortedBy { it.key.ordinal }.map { entry ->
                            listOfNotNull(
                                Category(
                                    type = entry.key
                                ).takeIf {
                                    entry.value.isNotEmpty()
                                }
                            ) + entry.value
                        }.flatten()
                    )
                }
            }.flowOn(dispatcher).launchIn(viewModelScope)
        }
    }

    @Suppress("MagicNumber")
    private fun disableRefreshing() {
        viewModelScope.launch {
            delay(300) // Without delay the UI might not update correctly
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun List<DAppWithDetails>.filterItems(
        filters: DAppFilters,
        directories: DAppDirectory?
    ): List<DAppWithDetails> = asSequence().filter { dAppWithDetails ->
        val directoryDefinition = directories?.findByAddress(dAppWithDetails.dAppDefinitionAddress)

        if (filters.selectedTags.isEmpty() || directoryDefinition == null) {
            true
        } else {
            directoryDefinition.tags.map { it.lowercase() }
                .containsAll(filters.selectedTags.map { it.lowercase() })
        }
    }.filter { dAppWithDetails ->
        val term = filters.searchTerm.trim().lowercase()

        if (term.isBlank()) {
            true
        } else {
            val directoryDefinition = directories?.findByAddress(dAppWithDetails.dAppDefinitionAddress)

            val (name, description) = when (val details = dAppWithDetails.details) {
                is DAppWithDetails.Details.Data -> {
                    val name = details.name.takeIf { it.isNotEmpty() } ?: directoryDefinition?.name
                    name to details.description
                }

                is DAppWithDetails.Details.Error -> {
                    directoryDefinition?.name to null
                }

                else -> null to null
            }

            if (name != null && name.lowercase().contains(term)) {
                return@filter true
            }

            if (description != null && description.lowercase().contains(term)) {
                return@filter true
            }

            if (name == null && description == null) {
                return@filter true
            }

            false
        }
    }.toList()

    interface ViewActions {

        fun onSearchTermUpdated(term: String)

        fun onFilterTagAdded(tag: String)

        fun onFilterTagRemoved(tag: String)

        fun onAllFilterTagsRemoved()
    }
}
