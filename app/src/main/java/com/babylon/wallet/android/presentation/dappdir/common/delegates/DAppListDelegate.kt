package com.babylon.wallet.android.presentation.dappdir.common.delegates

import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.DAppDirectory
import com.babylon.wallet.android.domain.usecases.GetDAppDirectoryUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppsUseCase
import com.babylon.wallet.android.domain.utils.AccountLockersObserver
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.dappdir.common.delegates.DAppListDelegate.ViewActions
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppCategoryType
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppFilters
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.Category
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListState
import com.babylon.wallet.android.presentation.discover.learn.partiallyMatches
import com.babylon.wallet.android.presentation.discover.learn.splitByWhitespace
import com.radixdlt.sargon.AccountAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.mapWhen
import rdx.works.core.sargon.isCurrentNetworkMainnet
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@Suppress("TooManyFunctions")
class DAppListDelegate @Inject constructor(
    private val getDAppDirectoryUseCase: GetDAppDirectoryUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getDAppsUseCase: GetDAppsUseCase,
    private val accountLockersObserver: AccountLockersObserver,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModelDelegate<DAppListState>(), ViewActions {

    val directoryState: MutableStateFlow<DAppDirectory?> = MutableStateFlow(null)
    val dAppDataState = MutableStateFlow<Map<AccountAddress, DAppWithDetails.Details>>(emptyMap())

    private val dAppsState = MutableStateFlow<Map<AccountAddress, DApp>>(emptyMap())

    private val _filtersState: MutableStateFlow<DAppFilters> = MutableStateFlow(DAppFilters())
    private val filtersState: Flow<DAppFilters> = _filtersState.onEach {
        _state.update { state -> state.copy(filters = it) }
    }

    private var loadDAppsJob: Job? = null
    private var accountLockerDepositsJob: Job? = null

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
        dAppsWithDetailsState: Flow<List<DAppWithDetails>>,
        observeAccountLockerDeposits: Boolean
    ) {
        super.invoke(scope, state)
        loadDAppDirectory()
        observeDAppsData(observeAccountLockerDeposits)
        observeStateChanges(dAppsWithDetailsState)
        observeNetworkGatewayChange()
    }

    fun loadDAppDirectory(onSuccess: ((DAppDirectory) -> Unit)? = null) {
        loadDAppsJob?.cancel()
        loadDAppsJob = getDAppDirectoryUseCase(
            isRefreshing = _state.value.isRefreshing
        ).onEach { result ->
            result.onFailure { error ->
                onDAppsLoadingError(error)
            }.onSuccess { directory ->
                directoryState.update { directory }
                onSuccess?.invoke(directory)
            }
        }.flowOn(dispatcher).launchIn(viewModelScope)
    }

    fun onDAppsLoaded(dAppDefinitionAddresses: List<AccountAddress>) {
        disableRefreshing()
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
                isRefreshing = false,
                isLoading = false,
                errorLoading = true
            )
        }
    }

    private fun observeDAppsData(observeAccountLockerDeposits: Boolean) {
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
                val dAppTags = { dApp: DApp ->
                    if (dApp.tags.isEmpty()) {
                        // If the dApp has no tags defined in metadata, we try to get them from the directory
                        directoryState.value?.findByAddress(dApp.dAppAddress)?.tags?.toSet().orEmpty()
                    } else {
                        dApp.tags
                    }
                }

                dAppsState.update { dAppDefinitionWithDetails }

                dAppDataState.update { data ->
                    data.mapValues {
                        val dApp = dAppDefinitionWithDetails[it.key]

                        if (dApp != null) {
                            DAppWithDetails.Details.Data(
                                name = dApp.name.orEmpty(),
                                iconUri = dApp.iconUrl,
                                description = dApp.description,
                                tags = dAppTags(dApp)
                            )
                        } else {
                            DAppWithDetails.Details.Error
                        }
                    }
                }

                if (observeAccountLockerDeposits) {
                    observeAccountLockerDeposits(dApps)
                }

                val availableTags = dApps.flatMap { dApp -> dAppTags(dApp) }
                    .map { it.lowercase() }.sorted().toSet()

                _filtersState.update { it.copy(availableTags = availableTags) }
            }
        }.catch { error ->
            onDAppsLoadingError(error)
        }.launchIn(viewModelScope)
    }

    private fun observeStateChanges(dAppsWithDetailsState: Flow<List<DAppWithDetails>>) {
        viewModelScope.launch {
            combine(
                directoryState,
                dAppsState,
                dAppsWithDetailsState,
                filtersState
            ) { directories, dApps, dAppsWithDetails, filters ->
                val items = dAppsWithDetails.filterItems(filters, directories)
                items.groupBy { item ->
                    val dApp = dApps[item.dAppDefinitionAddress]
                    val metadataDAppCategory = DAppCategoryType.from(dApp?.dAppCategory)

                    if (metadataDAppCategory == DAppCategoryType.Unknown) {
                        val directoryDefinition = directories?.findByAddress(item.dAppDefinitionAddress)
                        DAppCategoryType.from(directoryDefinition?.category)
                    } else {
                        metadataDAppCategory
                    }
                }
            }.onEach { itemsByCategory ->
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

    private fun observeNetworkGatewayChange() {
        viewModelScope.launch {
            getProfileUseCase.flow
                .distinctUntilChangedBy { it.isCurrentNetworkMainnet }
                .onEach {
                    // Reset filters when the network gateway is changed
                    _filtersState.update { DAppFilters() }
                }
                .launchIn(viewModelScope)
        }
    }

    @Suppress("MagicNumber")
    private fun disableRefreshing() {
        viewModelScope.launch {
            delay(300) // Without delay the UI might not update correctly
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun observeAccountLockerDeposits(dApps: List<DApp>) {
        accountLockerDepositsJob?.cancel()
        accountLockerDepositsJob = viewModelScope.launch {
            accountLockersObserver.depositsByAccount
                .map { it.values.flatten() }
                .collect { deposits ->
                    _state.update { state ->
                        val dAppDefinitionAddressesOfDAppsWithDeposits = dApps.filter { dApp ->
                            deposits.any { deposit -> deposit.lockerAddress == dApp.lockerAddress }
                        }.map {
                            it.dAppAddress
                        }

                        state.copy(
                            items = state.items.mapWhen(
                                predicate = {
                                    it is DAppWithDetails &&
                                        it.dAppDefinitionAddress in dAppDefinitionAddressesOfDAppsWithDeposits
                                },
                                mutation = {
                                    (it as DAppWithDetails).copy(
                                        hasDeposits = true
                                    )
                                }
                            )
                        )
                    }
                }
        }
    }

    private fun List<DAppWithDetails>.filterItems(
        filters: DAppFilters,
        directory: DAppDirectory?
    ): List<DAppWithDetails> {
        return asSequence().filterByTags(
            tags = filters.selectedTags,
            directory = directory
        ).filterByTerm(
            term = filters.searchTerm,
            directory = directory
        ).toList()
    }

    private fun Sequence<DAppWithDetails>.filterByTags(
        tags: Set<String>,
        directory: DAppDirectory?
    ): Sequence<DAppWithDetails> = filter { dAppWithDetails ->
        val directoryDefinition = directory?.findByAddress(dAppWithDetails.dAppDefinitionAddress)

        if (tags.isEmpty()) {
            true
        } else {
            dAppWithDetails.data?.tags?.containsAll(tags) == true ||
                directoryDefinition?.tags?.containsAll(tags) == true
        }
    }

    @Suppress("MagicNumber")
    private fun Sequence<DAppWithDetails>.filterByTerm(
        term: String,
        directory: DAppDirectory?
    ): Sequence<DAppWithDetails> {
        val normalizedTerm = term.trim().lowercase()
        val termParts = normalizedTerm.splitByWhitespace()

        return map { dAppWithDetails ->
            if (normalizedTerm.isBlank()) {
                dAppWithDetails to 0
            } else {
                val directoryDefinition = directory?.findByAddress(dAppWithDetails.dAppDefinitionAddress)
                val name = when (val details = dAppWithDetails.details) {
                    is DAppWithDetails.Details.Data -> details.name
                    DAppWithDetails.Details.Error -> directoryDefinition?.name
                    DAppWithDetails.Details.Fetching -> null
                }
                val description = dAppWithDetails.data?.description
                val tags = dAppWithDetails.data?.tags

                val relevance = when {
                    name != null && name.lowercase().contains(normalizedTerm) -> 0
                    description != null && description.lowercase().contains(normalizedTerm) -> 1
                    name?.splitByWhitespace().orEmpty().partiallyMatches(termParts) -> 2
                    description?.splitByWhitespace().orEmpty().partiallyMatches(termParts) -> 3
                    tags != null && tags.any { it.contains(normalizedTerm) } -> 4
                    else -> 5
                }

                dAppWithDetails to relevance
            }
        }.filter { it.second < 5 }
            .sortedBy { it.second }
            .map { it.first }
    }

    interface ViewActions {

        fun onSearchTermUpdated(term: String)

        fun onFilterTagAdded(tag: String)

        fun onFilterTagRemoved(tag: String)

        fun onAllFilterTagsRemoved()
    }
}
