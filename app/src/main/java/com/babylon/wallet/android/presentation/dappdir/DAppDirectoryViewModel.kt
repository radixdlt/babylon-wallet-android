package com.babylon.wallet.android.presentation.dappdir

import android.net.Uri
import android.util.Log
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
    private val filtersState: MutableStateFlow<DAppDirectoryFilters> =
        MutableStateFlow(DAppDirectoryFilters())
    private val tabState: MutableStateFlow<DAppDirectoryTab> = MutableStateFlow(DAppDirectoryTab.All)

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
    private val dAppsWithDetails: Flow<List<DAppWithDetails>> = tabState.onEach { tab ->
        _state.update { it.copy(selectedTab = tab) }
    }.flatMapLatest { tab ->
        when (tab) {
            DAppDirectoryTab.All -> directoryDAppsWithDetails
            DAppDirectoryTab.Approved -> approvedDAppsWithDetails
        }
    }

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
        dAppDataState.update { data -> data.mapValues { DAppWithDetails.Details.Fetching } }
        fetchDAppsDirectory()
        fetchApprovedDApps()
    }

    fun onTabSelected(tab: DAppDirectoryTab) {
        tabState.update { tab }
    }

    fun onSearchTermUpdated(term: String) {
        filtersState.update { it.copy(searchTerm = term) }
    }

    fun onFilterTagAdded(tag: String) {
        filtersState.update {
            it.copy(selectedTags = it.selectedTags + tag)
        }
    }

    fun onFilterTagRemoved(tag: String) {
        filtersState.update {
            it.copy(selectedTags = it.selectedTags - tag)
        }
    }

    fun onAllFilterTagsRemoved() {
        filtersState.update {
            it.copy(selectedTags = emptySet())
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    private fun fetchApprovedDApps() {
        dAppConnectionRepository.getAuthorizedDApps()
            .onEach { approvedDApps ->
                approvedDAppsState.update { approvedDApps }

                dAppDataState.update { dAppData ->
                    dAppData.toMutableMap()
                        .let { updatedDAppData ->
                            approvedDApps.forEach { dApp ->
                                // If the dApp is already present, we keep the existing details
                                updatedDAppData[dApp.dappDefinitionAddress] =
                                    dAppData[dApp.dappDefinitionAddress] ?: DAppWithDetails.Details.Fetching
                            }

                            updatedDAppData
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
        getDAppDirectoryUseCase(
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
                    data.toMutableMap()
                        .let { updatedDAppData ->
                            directory.all.forEach { dApp ->
                                // If the dApp is already present, we keep the existing details
                                updatedDAppData[dApp.dAppDefinitionAddress] =
                                    data[dApp.dAppDefinitionAddress] ?: DAppWithDetails.Details.Fetching
                            }

                            updatedDAppData
                        }
                }

                filtersState.update { filters ->
                    filters.copy(
                        availableTags = directory.all.map { it.tags }.flatten().toSet()
                    )
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
                Log.e("TAGTAG", "Fetch dApps for: $unknownDefinitions")
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
                filtersState.onEach { _state.update { state -> state.copy(filters = it) } }
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
                        items = itemsByCategory.map { entry ->
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
                        (details.name.takeIf { it.isNotEmpty() }
                            ?: directoryDefinition?.name) to details.description
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

    private fun disableRefreshing() {
        viewModelScope.launch {
            delay(300) // Without delay the UI might not update correctly
            _state.update { it.copy(isRefreshing = false) }
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

    enum class DAppCategoryType {

        Unknown;

        companion object {

            fun from(value: String?): DAppCategoryType = when (value?.lowercase()) {
                //TODO sergiu map categories to enum values
                else -> Unknown
            }
        }
    }

    enum class DAppDirectoryTab {

        All,
        Approved
    }
}
