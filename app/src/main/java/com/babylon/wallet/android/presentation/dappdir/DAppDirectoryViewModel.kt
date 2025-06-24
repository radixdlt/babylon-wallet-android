package com.babylon.wallet.android.presentation.dappdir

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DAppDirectory
import com.babylon.wallet.android.domain.usecases.GetDAppDirectoryUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AuthorizedDapp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.DAppConnectionRepository
import javax.inject.Inject
import kotlin.collections.asSequence
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.sequences.filter
import kotlin.text.lowercase

@HiltViewModel
class DAppDirectoryViewModel @Inject constructor(
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getDAppDirectoryUseCase: GetDAppDirectoryUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<DAppDirectoryViewModel.State>() {

    private val directoryState: MutableStateFlow<DAppDirectory?> = MutableStateFlow(null)
    private val approvedDAppsState: MutableStateFlow<List<AuthorizedDapp>> = MutableStateFlow(emptyList())

    private val dAppDataState: MutableStateFlow<Map<AccountAddress, DAppWithDetails.Details>> =
        MutableStateFlow(emptyMap())
    private val filtersByTabState: MutableStateFlow<Map<DAppDirectoryTab, DAppDirectoryFilters>> =
        MutableStateFlow(DAppDirectoryTab.entries.associate { it to DAppDirectoryFilters() })
    private val _tabState: MutableStateFlow<DAppDirectoryTab> = MutableStateFlow(DAppDirectoryTab.All)
    private val tabState = _tabState.onEach { tab ->
        _state.update { it.copy(selectedTab = tab) }
    }

    private val directoryDAppsWithDetails: Flow<List<DAppWithDetails>> = combine(
        directoryState,
        dAppDataState,
    ) { directory, dAppData ->
        directory?.highlighted.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                details = dAppData.getOrDefault(it.dAppDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        } + directory?.others.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                details = dAppData.getOrDefault(it.dAppDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        }
    }
    private val approvedDAppsWithDetails: Flow<List<DAppWithDetails>> = combine(
        approvedDAppsState,
        dAppDataState
    ) { approvedDApps, dAppData ->
        approvedDApps.map { dApp ->
            DAppWithDetails(
                dAppDefinitionAddress = dApp.dappDefinitionAddress,
                details = dAppDataState.value.getOrDefault(dApp.dappDefinitionAddress, DAppWithDetails.Details.Fetching)
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filtersState: Flow<DAppDirectoryFilters> = tabState.flatMapLatest { tab ->
        filtersByTabState.map { filtersByTab ->
            requireNotNull(filtersByTab[tab])
        }.onEach { _state.update { state -> state.copy(filters = it) } }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dAppsWithDetails: Flow<List<DAppWithDetails>> = tabState.flatMapLatest { tab ->
        when (tab) {
            DAppDirectoryTab.All -> directoryDAppsWithDetails
            DAppDirectoryTab.Approved -> approvedDAppsWithDetails
        }
    }

    private var fetchDAppsDirectoryJob: Job? = null
    private var fetchApprovedDAppsJob: Job? = null

    init {
        fetchDAppsDirectory()
        fetchApprovedDApps()
        observeDAppsData()
        observeStateChanges()
    }

    override fun initialState(): State = State(
        isLoading = true,
        isRefreshing = false,
        errorLoadingDirectory = false,
        errorLoadingApprovedDApps = false
    )

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true) }
        fetchDAppsDirectory()
        fetchApprovedDApps()
    }

    fun onTabSelected(tab: DAppDirectoryTab) {
        _tabState.update { tab }
    }

    fun onSearchTermUpdated(term: String) {
        updateFiltersState { it.copy(searchTerm = term) }
    }

    fun onFilterTagAdded(tag: String) {
        updateFiltersState {
            it.copy(selectedTags = it.selectedTags + tag)
        }
    }

    fun onFilterTagRemoved(tag: String) {
        updateFiltersState {
            it.copy(selectedTags = it.selectedTags - tag)
        }
    }

    fun onAllFilterTagsRemoved() {
        updateFiltersState {
            it.copy(selectedTags = emptySet())
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun fetchApprovedDApps() {
        fetchApprovedDAppsJob?.cancel()
        fetchApprovedDAppsJob = dAppConnectionRepository.getAuthorizedDApps()
            .onEach { approvedDApps ->
                approvedDAppsState.update { approvedDApps }

                dAppDataState.update { data ->
                    data.toMutableMap().apply {
                        putAll(
                            approvedDApps.associate {
                                it.dappDefinitionAddress to DAppWithDetails.Details.Fetching
                            }
                        )
                    }
                }
            }
            .catch { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        isLoading = false,
                        errorLoadingApprovedDApps = true
                    )
                }
            }
            .flowOn(dispatcher)
            .launchIn(viewModelScope)
    }

    private fun fetchDAppsDirectory() {
        fetchDAppsDirectoryJob?.cancel()
        fetchDAppsDirectoryJob = getDAppDirectoryUseCase(
            isRefreshing = state.value.isRefreshing
        ).onEach { result ->
            result.onFailure { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        isLoading = false,
                        errorLoadingDirectory = true
                    )
                }
            }.onSuccess { directory ->
                directoryState.update {
                    directory.copy(
                        // Shuffle order of highlighted dApps
                        highlighted = directory.highlighted?.shuffled(),
                        // Shuffle order of other dApps
                        others = directory.others?.shuffled()
                    )
                }

                dAppDataState.update { data ->
                    data.toMutableMap().apply {
                        putAll(
                            directory.all.associate {
                                it.dAppDefinitionAddress to DAppWithDetails.Details.Fetching
                            }
                        )
                    }
                }

                val availableTags = directory.all.map { it.tags }.flatten().toSet()
                DAppDirectoryTab.entries.forEach { tab ->
                    updateFiltersState(tab) { filters ->
                        filters.copy(availableTags = availableTags)
                    }
                }
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    private fun observeDAppsData() {
        dAppDataState
            .map { data ->
                data.filter { it.value is DAppWithDetails.Details.Fetching }
                    .keys.toSet()
            }
            .filter { unknownDefinitions -> unknownDefinitions.isNotEmpty() }
            .onEach { unknownDefinitions ->
                getDAppsUseCase(
                    definitionAddresses = unknownDefinitions,
                    needMostRecentData = state.value.isRefreshing
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
                }
            }.catch { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(error),
                        isLoading = false,
                        errorLoadingApprovedDApps = true,
                        errorLoadingDirectory = true
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun observeStateChanges() {
        viewModelScope.launch {
            combine(
                directoryState,
                dAppsWithDetails,
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

    private fun List<DAppWithDetails>.filterItems(
        filters: DAppDirectoryFilters,
        directories: DAppDirectory?
    ): List<DAppWithDetails> = asSequence()
        .filter { dAppWithDetails ->
            val directoryDefinition = directories?.findByAddress(dAppWithDetails.dAppDefinitionAddress)

            if (filters.selectedTags.isEmpty() || directoryDefinition == null) {
                true
            } else {
                directoryDefinition.tags.map { it.lowercase() }
                    .containsAll(filters.selectedTags.map { it.lowercase() })
            }
        }
        .filter { dAppWithDetails ->
            val term = filters.searchTerm.trim().lowercase()

            if (term.isBlank()) {
                true
            } else {
                val directoryDefinition = directories?.findByAddress(dAppWithDetails.dAppDefinitionAddress)

                val (name, description) = when (val details = dAppWithDetails.details) {
                    is DAppWithDetails.Details.Data -> {
                        (
                            details.name.takeIf { it.isNotEmpty() }
                                ?: directoryDefinition?.name
                            ) to details.description
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

    @Suppress("MagicNumber")
    private fun disableRefreshing() {
        viewModelScope.launch {
            delay(300) // Without delay the UI might not update correctly
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateFiltersState(
        forTab: DAppDirectoryTab = state.value.selectedTab,
        update: (DAppDirectoryFilters) -> DAppDirectoryFilters
    ) {
        filtersByTabState.update { filtersByTab ->
            filtersByTab.mapValues { (tab, currentValues) ->
                if (tab == forTab) {
                    update(currentValues)
                } else {
                    currentValues
                }
            }
        }
    }

    data class State(
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val errorLoadingApprovedDApps: Boolean = false,
        val errorLoadingDirectory: Boolean = false,
        val items: List<Item> = emptyList(),
        val filters: DAppDirectoryFilters = DAppDirectoryFilters(),
        val uiMessage: UiMessage? = null,
        val selectedTab: DAppDirectoryTab = DAppDirectoryTab.All
    ) : UiState {

        val isFiltersButtonVisible: Boolean = !isLoading && filters.availableTags.isNotEmpty()
        val errorLoading: Boolean = when (selectedTab) {
            DAppDirectoryTab.All -> errorLoadingDirectory
            DAppDirectoryTab.Approved -> errorLoadingApprovedDApps
        }
        val isEmpty: Boolean = items.isEmpty() && !isLoading && !errorLoading && filters.searchTerm.isBlank()
    }

    sealed interface Item

    data class Category(
        val type: DAppCategoryType
    ) : Item

    data class DAppWithDetails(
        val dAppDefinitionAddress: AccountAddress,
        val details: Details
    ) : Item {

        val isFetchingDAppDetails: Boolean = details is Details.Fetching
        val data: Details.Data? = details as? Details.Data

        sealed interface Details {

            data object Fetching : Details

            data object Error : Details

            data class Data(
                val name: String,
                val iconUri: Uri?,
                val description: String?
            ) : Details
        }

        companion object
    }

    data class DAppDirectoryFilters(
        val searchTerm: String = "",
        val selectedTags: Set<String> = emptySet(),
        val availableTags: Set<String> = emptySet()
    ) {

        fun isTagSelected(tag: String) = tag in selectedTags
    }

    enum class DAppCategoryType(val value: String) {

        DeFi("DeFi"),
        Utility("Utility"),
        Dao("Dao"),
        NFT("NFT"),
        Meme("Meme"),
        Unknown("Other");

        companion object {

            fun from(value: String?): DAppCategoryType = entries.firstOrNull {
                it.name.equals(
                    other = value,
                    ignoreCase = true
                )
            } ?: Unknown
        }
    }

    enum class DAppDirectoryTab {

        All,
        Approved
    }
}
