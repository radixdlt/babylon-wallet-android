package com.babylon.wallet.android.presentation.dappdir

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DAppDirectory
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import com.babylon.wallet.android.domain.usecases.GetDAppDirectoryUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.string
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
import rdx.works.core.domain.DApp
import rdx.works.core.then
import javax.inject.Inject

@HiltViewModel
class DAppDirectoryViewModel @Inject constructor(
    val getDAppDirectoryUseCase: GetDAppDirectoryUseCase,
    val getDAppsUseCase: GetDAppsUseCase,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : StateViewModel<DAppDirectoryViewModel.State>() {

    private val directoryState: MutableStateFlow<DAppDirectory?> = MutableStateFlow(null)
    private val dAppDataState: MutableStateFlow<Map<AccountAddress, DirectoryDAppWithDetails.Details>> =
        MutableStateFlow(emptyMap())
    private val filtersState: MutableStateFlow<DAppDirectoryFilters> =
        MutableStateFlow(DAppDirectoryFilters())

    private val directoryDAppsWithDetails: Flow<List<DirectoryDAppWithDetails>> = combine(
        directoryState,
        dAppDataState,
    ) { directory, dAppData ->
        directory?.highlighted.orEmpty().map {
            DirectoryDAppWithDetails(
                directoryDefinition = it,
                isHighlighted = true,
                details = dAppData.getOrDefault(
                    it.dAppDefinitionAddress,
                    DirectoryDAppWithDetails.Details.Fetching
                )
            )
        } + directory?.others.orEmpty().map {
            DirectoryDAppWithDetails(
                directoryDefinition = it,
                isHighlighted = false,
                details = dAppData.getOrDefault(
                    it.dAppDefinitionAddress,
                    DirectoryDAppWithDetails.Details.Fetching
                )
            )
        }
    }

    override fun initialState(): State = State(
        isLoadingDirectory = true,
        isRefreshing = false,
        errorLoadingDirectory = false
    )

    init {
        viewModelScope.launch {
            combine(
                directoryDAppsWithDetails,
                filtersState.onEach { _state.update { state -> state.copy(filters = it) } }
            ) { dApps, filters ->
                dApps
                    .asSequence()
                    .filter { directoryDApp ->
                        if (filters.selectedTags.isEmpty()) {
                            true
                        } else {
                            directoryDApp.tags.map { it.lowercase() }
                                .containsAll(filters.selectedTags.map { it.lowercase() })
                        }
                    }
                    .filter { directoryDApp ->
                        val term = filters.searchTerm.trim().lowercase()

                        if (term.isBlank()) {
                            true
                        } else {
                            val (name, description) = when (val details = directoryDApp.details) {
                                is DirectoryDAppWithDetails.Details.Data ->
                                    (details.dApp.name ?: directoryDApp.directoryDefinition.name) to
                                        details.dApp.description

                                is DirectoryDAppWithDetails.Details.Error ->
                                    directoryDApp.directoryDefinition.name to null

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
            }.onEach { directory ->
                _state.update { state ->
                    state.copy(directory = directory)
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
                    dApp.dAppDefinitionAddress to DirectoryDAppWithDetails.Details.Fetching
                }
            }

            filtersState.update { filters ->
                filters.copy(
                    availableTags = directory.all.map { it.tags }.flatten().toSet()
                )
            }

            _state.update {
                it.copy(isLoadingDirectory = false)
            }

            dAppDataState.value.filter { it.value !is DirectoryDAppWithDetails.Details.Data }
                .keys
                .toSet()
        }.then { unknownDefinitions ->
            _state.update { it.copy(errorLoadingDirectory = false) }
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
                        DirectoryDAppWithDetails.Details.Data(dApp)
                    } else {
                        DirectoryDAppWithDetails.Details.Error
                    }
                }
            }
        }.onFailure { error ->
            dAppDataState.update { data ->
                data.mapValues {
                    if (it.value is DirectoryDAppWithDetails.Details.Fetching) {
                        DirectoryDAppWithDetails.Details.Error
                    } else {
                        it.value
                    }
                }
            }

            _state.update {
                it.copy(
                    isRefreshing = false,
                    isLoadingDirectory = false,
                    errorLoadingDirectory = it.isDirectoryEmpty,
                    uiMessage = if (!it.isDirectoryEmpty) UiMessage.ErrorMessage(error) else null
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

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    data class State(
        val isLoadingDirectory: Boolean,
        val isRefreshing: Boolean,
        val errorLoadingDirectory: Boolean,
        val directory: List<DirectoryDAppWithDetails> = emptyList(),
        val filters: DAppDirectoryFilters = DAppDirectoryFilters(),
        val uiMessage: UiMessage? = null
    ) : UiState {

        val isDirectoryEmpty: Boolean = directory.isEmpty()
    }
}

data class DirectoryDAppWithDetails(
    val directoryDefinition: DirectoryDefinition,
    val isHighlighted: Boolean,
    val details: Details
) {

    val key: String = directoryDefinition.dAppDefinitionAddress.string

    val dApp: DApp? = (details as? Details.Data)?.dApp

    val isFetchingDAppDetails: Boolean = details is Details.Fetching

    val tags: List<String> = directoryDefinition.tags

    sealed interface Details {
        data object Fetching : Details

        data object Error : Details

        data class Data(val dApp: DApp) : Details
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
