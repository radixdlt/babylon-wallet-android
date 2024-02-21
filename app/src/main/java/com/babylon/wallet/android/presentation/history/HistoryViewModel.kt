package com.babylon.wallet.android.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryData
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.assets.GetAccountFirstTransactionDateUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetAccountHistoryUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.toMonthString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.apppreferences.Radix.dashboardUrl
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountHistoryUseCase: GetAccountHistoryUseCase,
    private val getAccountFirstTransactionDateUseCase: GetAccountFirstTransactionDateUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<State>(), OneOffEventHandler<HistoryEvent> by OneOffEventHandlerImpl() {

    private val args = HistoryArgs(savedStateHandle)
    override fun initialState(): State = State()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(args.accountAddress)?.let { account ->
                _state.update {
                    it.copy(accountWithAssets = AccountWithAssets(account))
                }
                getWalletAssetsUseCase(listOf(account), false).catch { error ->
                    _state.update {
                        it.copy(isRefreshing = false, uiMessage = UiMessage.ErrorMessage(error = error))
                    }
                }.mapNotNull { it.firstOrNull() }.collectLatest { accountWithAssets ->
                    _state.update { state ->
                        state.copy(
                            accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets),
                            filterResources = accountWithAssets.assets?.knownResources?.map { Selectable(it) }.orEmpty().toPersistentList()
                        )
                    }
                    if (accountWithAssets.details?.firstTransactionDate == null) {
                        getAccountFirstTransactionDateUseCase(args.accountAddress)
                    } else {
                        computeTimeFilters(accountWithAssets.details.firstTransactionDate)
                    }
                }
            }
        }
        loadPage()
    }

    private fun loadPage(delayMs: Long = 0, newSearch: Boolean = false) {
        searchJob = viewModelScope.launch {
            delay(delayMs)
            if (isActive.not()) return@launch
            getAccountHistoryUseCase.invoke(args.accountAddress, if (newSearch) null else _state.value.historyData, _state.value.filters)
                .onSuccess { historyData ->
                    val historyItems = mutableListOf<HistoryItem>()
                    historyData.groupedByDate.forEach { (_, transactionItems) ->
                        val date = transactionItems.first().timestamp?.atZone(ZonedDateTime.now().zone) ?: ZonedDateTime.now()
                        historyItems.add(HistoryItem.Date(date))
                        transactionItems.forEach { historyItems.add(HistoryItem.Transaction(it)) }
                    }
                    _state.update {
                        it.copy(
                            content = if (historyItems.isEmpty()) {
                                Content.Empty
                            } else {
                                Content.Loaded(
                                    historyData = historyData,
                                    historyItems = historyItems.toPersistentList()
                                )
                            },
                            isLoadingMore = false
                        )
                    }
                }.onFailure {
                }
        }
    }

    @Suppress("MagicNumber")
    private fun computeTimeFilters(firstTransaction: Instant) {
        val result = mutableListOf<TimeFilterItem>()
        val now = ZonedDateTime.now()
        val firstTransactionDate = ZonedDateTime.ofInstant(firstTransaction, now.zone)
        var start = ZonedDateTime.of(firstTransactionDate.year, firstTransactionDate.monthValue, 1, 0, 0, 0, 0, now.zone)
        var addedYear = 0
        while (start.isBefore(now)) {
            if (start.year != addedYear) {
                addedYear = start.year
                result.add(TimeFilterItem.Year(start.year))
            }
            val tempEnd = start.with(TemporalAdjusters.lastDayOfMonth())
            var end = ZonedDateTime.of(tempEnd.year, tempEnd.monthValue, tempEnd.dayOfMonth, 23, 59, 59, 999, now.zone)
            if (end.isAfter(now)) end = now
            result.add(
                TimeFilterItem.Month(
                    start.toMonthString(),
                    start,
                    end
                )
            )
            start = start.plusMonths(1)
        }
        _state.update { state ->
            state.copy(timeFilterItems = result.map { Selectable(it) }.toPersistentList())
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onShowFiltersDialog(visible: Boolean) {
        _state.update { it.copy(showFiltersSheet = visible) }
    }

    fun onClearAllFilters() {
        _state.update {
            it.copy(filters = HistoryFilters(start = it.filters.start, end = it.filters.end))
        }
    }

    fun onShowResults() {
        if (_state.value.filtersChanged.not()) {
            Timber.d("History: Filters are the same, skipping load")
            return
        }
        _state.update { it.copy(showFiltersSheet = false, content = Content.Loading) }
        loadPage(newSearch = true)
    }

    fun onTimeFilterSelected(timeFilterItem: TimeFilterItem.Month) {
//        _state.update { state ->
//            val updateItems = state.timeFilterItems.map {
//                if (it.data == timeFilterItem) it.copy(selected = !it.selected) else it.copy(selected = false)
//            }.toPersistentList()
//            val timeFilter = updateItems.find { it.selected }?.data as? TimeFilterItem.Month
//            state.copy(
//                timeFilterItems = updateItems,
//                filters = state.filters.copy(start = timeFilter?.start, end = timeFilter?.end)
//            )
//        }
//        if (_state.value.historyData?.currentFilters == _state.value.filters) {
//            Timber.d("History: Filters are the same, skipping load")
//            return
//        }
//        loadPage(delayMs = DEBOUNCE_DELAY_MS, newSearch = true)
    }

    fun onOpenTransactionDetails(txId: String) {
        viewModelScope.launch {
            state.value.accountWithAssets?.let { account ->
                val dashboardUrl = NetworkId.from(account.account.networkID).dashboardUrl()
                sendEvent(HistoryEvent.OnTransactionItemClick("$dashboardUrl/transaction/$txId/summary"))
            }
        }
    }

    fun onLoadMore() {
        if (_state.value.canLoadMore.not()) {
            Timber.d("History: Nothing to load")
            return
        }
        Timber.d("History: Load more")
        _state.update { it.copy(isLoadingMore = true) }
        loadPage()
    }

    fun onScrollEvent(event: ScrollInfo) {
        val items = _state.value.historyItems ?: return
        val firstVisibleItemDate = event.firstVisible?.let { items.getOrNull(it) }?.dateTime ?: return
        val lastVisibleItemDate = event.lastVisible?.let { items.getOrNull(it) }?.dateTime ?: return
        Timber.d("History: First item date: $firstVisibleItemDate, last item date: $lastVisibleItemDate")
        when (event.direction) {
            ScrollInfo.Direction.UP -> {
                selectDate(firstVisibleItemDate)
            }

            ScrollInfo.Direction.DOWN -> {
                selectDate(lastVisibleItemDate)
            }

            else -> {}
        }
    }

    fun onTransactionTypeFilterSelected(transactionType: HistoryFilters.TransactionType?) {
        _state.update { state ->
            val updatedFilters = state.filters.copy(
                transactionType = transactionType
            )
            state.copy(filters = updatedFilters)
        }
    }

    fun onTransactionClassFilterSelected(transactionClass: TransactionClass?) {
        _state.update { state ->
            val updatedFilters = state.filters.copy(
                transactionClass = transactionClass
            )
            state.copy(filters = updatedFilters)
        }
    }

    fun onResourceFilterSelected(resource: Resource) {
        _state.update { state ->
            val addresses = state.filters.resources.map { it.resourceAddress }
            val containsFilter = addresses.contains(resource.resourceAddress)
            val updatedFilters = state.filters.copy(
                resources = if (containsFilter) {
                    state.filters.resources - resource
                } else {
                    state.filters.resources + resource
                }
            )
            state.copy(filters = updatedFilters)
        }
    }

    fun onSubmittedByFilterChanged(value: HistoryFilters.SubmittedBy?) {
        _state.update { state ->
            state.copy(filters = state.filters.copy(submittedBy = value))
        }
    }

    private fun selectDate(date: ZonedDateTime?) {
        val selectedDate = date ?: return
        _state.update { state ->
            val updateItems = state.timeFilterItems.map { filter ->
                when (filter.data) {
                    is TimeFilterItem.Year -> filter.copy(selected = false)
                    is TimeFilterItem.Month -> {
                        if ((selectedDate.isAfter(filter.data.start) && selectedDate.isBefore(filter.data.end)) ||
                            filter.data.start.isEqual(date)
                        ) {
                            filter.copy(selected = true)
                        } else {
                            filter.copy(selected = false)
                        }
                    }
                }
            }.toPersistentList()
            state.copy(
                timeFilterItems = updateItems
            )
        }
    }
}

internal sealed interface HistoryEvent : OneOffEvent {
    data class OnTransactionItemClick(val url: String) : HistoryEvent
}

data class State(
    val accountWithAssets: AccountWithAssets? = null,
    val content: Content = Content.Loading,
    val epoch: Long? = null,
    val isRefreshing: Boolean = false,
    val uiMessage: UiMessage? = null,
    val showFiltersSheet: Boolean = false,
    val filters: HistoryFilters = HistoryFilters(),
    val timeFilterItems: ImmutableList<Selectable<TimeFilterItem>> = persistentListOf(),
    val filterResources: ImmutableList<Selectable<Resource>> = persistentListOf(),
    val isLoadingMore: Boolean = false
) : UiState {

    val historyData
        get() = when (content) {
            is Content.Loaded -> content.historyData
            else -> null
        }

    val historyItems
        get() = when (content) {
            is Content.Loaded -> content.historyItems
            else -> null
        }

    private val currentFilters: HistoryFilters?
        get() = when (content) {
            is Content.Loaded -> content.historyData.currentFilters
            else -> null
        }

    val fungibleResources: List<Resource.FungibleResource>
        get() = accountWithAssets?.assets?.knownFungibles.orEmpty()

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = accountWithAssets?.assets?.knownNonFungibles.orEmpty()

    val canLoadMore: Boolean
        get() = content is Content.Loaded && content.historyData.nextCursorId != null

    val filtersChanged: Boolean
        get() = filters != currentFilters
}

sealed class Content {
    data object Loading : Content()
    data object Empty : Content()
    data class Loaded(
        val historyData: TransactionHistoryData,
        val historyItems: ImmutableList<HistoryItem>
    ) : Content()
}

sealed class TimeFilterItem {
    data class Year(val year: Int) : TimeFilterItem()
    data class Month(val month: String, val start: ZonedDateTime, val end: ZonedDateTime) : TimeFilterItem()
}

sealed interface HistoryItem {

    val dateTime: ZonedDateTime?

    data class Date(val item: ZonedDateTime) : HistoryItem {
        override val dateTime: ZonedDateTime
            get() = item
    }

    data class Transaction(val transactionItem: TransactionHistoryItem) : HistoryItem {
        override val dateTime: ZonedDateTime?
            get() = transactionItem.timestamp?.atZone(ZoneId.systemDefault())
    }
}
