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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.then
import javax.inject.Inject

@HiltViewModel
class DAppDirectoryViewModel @Inject constructor(
    private val getDAppDirectoryUseCase: GetDAppDirectoryUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<DAppDirectoryViewModel.State>() {

    private val directoryState: MutableStateFlow<DAppDirectory?> = MutableStateFlow(null)
    private val dAppDataState: MutableStateFlow<Map<AccountAddress, DAppWithDetails.Details>> =
        MutableStateFlow(emptyMap())
    private val filtersState: MutableStateFlow<DAppDirectoryFilters> =
        MutableStateFlow(DAppDirectoryFilters())

    private val directoryDAppsWithDetails: Flow<List<DAppWithDetails>> = combine(
        directoryState,
        dAppDataState,
    ) { directory, dAppData ->
        directory?.highlighted.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                details = dAppData.getOrDefault(
                    it.dAppDefinitionAddress,
                    DAppWithDetails.Details.Fetching
                )
            )
        } + directory?.others.orEmpty().map {
            DAppWithDetails(
                dAppDefinitionAddress = it.dAppDefinitionAddress,
                details = dAppData.getOrDefault(
                    it.dAppDefinitionAddress,
                    DAppWithDetails.Details.Fetching
                )
            )
        }
    }

    override fun initialState(): State = State(
        isLoading = true,
        isRefreshing = false,
        errorLoading = false
    )

    init {
        viewModelScope.launch {
            combine(
                directoryState,
                directoryDAppsWithDetails,
                filtersState.onEach { _state.update { state -> state.copy(filters = it) } }
            ) { directories, dAppsWithDetails, filters ->
                val items = dAppsWithDetails.asSequence()
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

                items to directories
            }.onEach { itemsAndDirectories ->
                val itemsByCategory = itemsAndDirectories.first.groupBy { item ->
                    val directoryDefinition = itemsAndDirectories.second?.findByAddress(item.dAppDefinitionAddress)
                    DAppCategoryType.from(directoryDefinition?.category)
                }

                _state.update { state ->
                    state.copy(
                        items = if (itemsByCategory.size == 1) {
                            // If all items are in the same category, we don't need to show the category header
                            itemsAndDirectories.first
                        } else {
                            itemsByCategory.map { entry ->
                                listOfNotNull(
                                    Category(
                                        type = entry.key
                                    ).takeIf {
                                        entry.value.isNotEmpty()
                                    }
                                ) + entry.value
                            }.flatten()
                        }
                    )
                }
            }.flowOn(dispatcher).launchIn(viewModelScope)
        }

        viewModelScope.launch {
            fetchDAppsDirectory()
        }
    }

    @Suppress("LongMethod")
    private suspend fun fetchDAppsDirectory() {
        getDAppDirectoryUseCase(isRefreshing = state.value.isRefreshing).map { directory ->
            directoryState.update {
                directory.copy(
                    // Shuffle order of highlighted dApps
                    highlighted = directory.highlighted?.shuffled(),
                    // Shuffle order of other dApps
                    others = directory.others?.shuffled()
                )
            }

            dAppDataState.update {
                directory.all.associate { dApp ->
                    dApp.dAppDefinitionAddress to DAppWithDetails.Details.Fetching
                }
            }

            filtersState.update { filters ->
                filters.copy(
                    availableTags = directory.all.map { it.tags }.flatten().toSet()
                )
            }

            _state.update {
                it.copy(isLoading = false)
            }

            dAppDataState.value.filter { it.value !is DAppWithDetails.Details.Data }
                .keys
                .toSet()
        }.then { unknownDefinitions ->
            _state.update { it.copy(errorLoading = false) }
            getDAppsUseCase(
                definitionAddresses = unknownDefinitions,
                needMostRecentData = state.value.isRefreshing
            )
        }.onSuccess { dApps ->
            _state.update { it.copy(isRefreshing = false) }

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
        }.onFailure { error ->
            dAppDataState.update { data ->
                data.mapValues {
                    if (it.value is DAppWithDetails.Details.Fetching) {
                        DAppWithDetails.Details.Error
                    } else {
                        it.value
                    }
                }
            }

            _state.update {
                it.copy(
                    isRefreshing = false,
                    isLoading = false,
                    errorLoading = it.itemsEmpty,
                    uiMessage = if (!it.itemsEmpty) UiMessage.ErrorMessage(error) else null
                )
            }
        }
    }

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            fetchDAppsDirectory()
        }
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

    data class State(
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val errorLoading: Boolean,
        val items: List<Item> = emptyList(),
        val filters: DAppDirectoryFilters = DAppDirectoryFilters(),
        val uiMessage: UiMessage? = null
    ) : UiState {

        val itemsEmpty: Boolean = items.isEmpty()
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
}
