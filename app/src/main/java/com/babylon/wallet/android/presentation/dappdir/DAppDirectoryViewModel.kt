package com.babylon.wallet.android.presentation.dappdir

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import com.babylon.wallet.android.domain.usecases.GetDAppDirectoryUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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

    private val directoryData: MutableStateFlow<Map<DirectoryDefinition, DirectoryDAppWithDetails.Details>> =
        MutableStateFlow(emptyMap())
    private val filters: MutableStateFlow<DAppDirectoryFilters> =
        MutableStateFlow(DAppDirectoryFilters())

    override fun initialState(): State = State(
        isLoadingDirectory = true,
        isRefreshing = false,
        errorLoadingDirectory = false
    )

    init {
        viewModelScope.launch {
            combine(
                directoryData,
                filters.onEach { _state.update { state -> state.copy(filters = it) } }
            ) { data, filters ->
                data
                    .asSequence()
                    .filter { (definition, _) ->
                        if (filters.selectedTags.isEmpty()) {
                            true
                        } else {
                            definition.tags.map { it.lowercase() }
                                .containsAll(filters.selectedTags.map { it.lowercase() })
                        }
                    }
                    .filter { (definition, details) ->
                        val term = filters.searchTerm.trim().lowercase()

                        if (term.isBlank()) {
                            true
                        } else {
                            val (name, description) = when (details) {
                                is DirectoryDAppWithDetails.Details.Data ->
                                    (
                                            details.dApp.name
                                                ?: definition.name
                                            ) to details.dApp.description

                                is DirectoryDAppWithDetails.Details.Error ->
                                    definition.name to null

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
                    }
                    .toList()
                    .map { entry ->
                        DirectoryDAppWithDetails(
                            directoryDefinition = entry.key,
                            details = entry.value
                        )
                    }
            }.onEach { directory ->
                _state.update { state ->
                    state.copy(
                        directory = directory
                    )
                }
            }.flowOn(dispatcher).launchIn(viewModelScope)
        }

        viewModelScope.launch {
            fetchDAppsDirectory()
        }
    }

    private suspend fun fetchDAppsDirectory() {
        getDAppDirectoryUseCase(isRefreshing = state.value.isRefreshing).map { directory ->
            directoryData.update {
                directory.associateWith {
                    DirectoryDAppWithDetails.Details.Fetching
                }
            }

            filters.update { filters ->
                filters.copy(
                    availableTags = directory.map { it.tags }.flatten().toSet()
                )
            }

            _state.update {
                it.copy(isLoadingDirectory = false)
            }

            directoryData.value.filter { it.value !is DirectoryDAppWithDetails.Details.Data }
                .keys
                .map { it.dAppDefinitionAddress }
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

            directoryData.update { data ->
                data.mapValues {
                    val dApp = dAppDefinitionWithDetails[it.key.dAppDefinitionAddress]

                    if (dApp != null) {
                        DirectoryDAppWithDetails.Details.Data(dApp)
                    } else {
                        DirectoryDAppWithDetails.Details.Error
                    }
                }
            }
        }.onFailure { error ->
            directoryData.update { data ->
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
                    errorLoadingDirectory = it.directory.isEmpty(),
                    uiMessage = if (it.directory.isNotEmpty()) UiMessage.ErrorMessage(error) else null
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
        filters.update { it.copy(searchTerm = term) }
    }

    fun onFilterTagAdded(tag: String) {
        filters.update {
            it.copy(selectedTags = it.selectedTags + tag)
        }
    }

    fun onFilterTagRemoved(tag: String) {
        filters.update {
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
    ) : UiState
}

data class DirectoryDAppWithDetails(
    val directoryDefinition: DirectoryDefinition,
    val details: Details
) {
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
