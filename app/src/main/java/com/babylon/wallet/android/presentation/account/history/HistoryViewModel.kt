package com.babylon.wallet.android.presentation.account.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryData
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.usecases.assets.GetAccountHistoryUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetWalletAssetsUseCase
import com.babylon.wallet.android.domain.usecases.assets.UpdateAccountFirstTransactionDateUseCase
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
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

@Suppress("TooManyFunctions")
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountHistoryUseCase: GetAccountHistoryUseCase,
    private val updateAccountFirstTransactionDateUseCase: UpdateAccountFirstTransactionDateUseCase,
    private val getWalletAssetsUseCase: GetWalletAssetsUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<State>(), OneOffEventHandler<HistoryEvent> by OneOffEventHandlerImpl() {

    private val args = HistoryArgs(savedStateHandle)
    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            getProfileUseCase.accountOnCurrentNetwork(args.accountAddress)?.let { account ->
                _state.update {
                    it.copy(accountWithAssets = AccountWithAssets(account))
                }
                getWalletAssetsUseCase(listOf(account), false).catch { error ->
                    _state.update {
                        it.copy(uiMessage = UiMessage.ErrorMessage(error = error))
                    }
                }.mapNotNull { it.firstOrNull() }.collectLatest { accountWithAssets ->
                    _state.update { state ->
                        state.copy(
                            accountWithAssets = state.accountWithAssets?.copy(assets = accountWithAssets.assets)
                        )
                    }

                    accountWithAssets.details?.firstTransactionDate?.let { firstTransactionDate ->
                        computeTimeFilters(firstTransactionDate)
                    } ?: updateAccountFirstTransactionDateUseCase(args.accountAddress)
                }
            }
        }
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getAccountHistoryUseCase.getHistory(args.accountAddress, _state.value.filters).onSuccess { historyData ->
                updateStateWith(historyData)
                _state.value.historyItems?.firstOrNull()?.dateTime?.let { selectDate(it) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        uiMessage = UiMessage.ErrorMessage(error = error),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private fun computeTimeFilters(firstTransaction: Instant) {
        val result = mutableListOf<State.MonthFilter>()
        val now = ZonedDateTime.now()
        var rangeStart = ZonedDateTime.ofInstant(firstTransaction, now.zone)
        while (rangeStart.isBefore(now)) {
            val temp = rangeStart.with(TemporalAdjusters.lastDayOfMonth())
            var rangeEnd = ZonedDateTime.of(temp.year, temp.monthValue, temp.dayOfMonth, 23, 59, 59, 999, now.zone)
            if (rangeEnd.isAfter(now)) rangeEnd = now
            result.add(
                State.MonthFilter(
                    rangeStart.toMonthString().uppercase(),
                    rangeStart,
                    rangeEnd
                )
            )
            var newDate = rangeStart.plusMonths(1)
            newDate = ZonedDateTime.of(newDate.year, newDate.monthValue, 1, 0, 0, 0, 0, now.zone)
            rangeStart = newDate
        }
        _state.update { state ->
            state.copy(
                timeFilterItems = result.map { monthFilter ->
                    Selectable(monthFilter)
                }.toPersistentList()
            )
        }
        // if filters are computed after history loaded, we need to highlight current month
        if (_state.value.content is State.Content.Loaded) {
            _state.value.firstVisibleIndex?.let { index ->
                _state.value.historyItems?.getOrNull(index)?.dateTime?.let { selectDate(it) }
            } ?: _state.value.historyItems?.firstOrNull()?.dateTime?.let { selectDate(it) }
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(uiMessage = null) }
    }

    fun onRefresh() {
        _state.update { it.copy(isRefreshing = true, content = State.Content.Loading) }
        loadHistory()
    }

    fun onShowFiltersDialog(visible: Boolean) {
        _state.update { it.copy(shouldShowFiltersSheet = visible) }
    }

    fun onClearAllFilters() {
        _state.update {
            it.copy(filters = HistoryFilters(start = it.filters.start))
        }
    }

    fun onShowResults() {
        if (_state.value.filtersChanged.not()) {
            Timber.d("History: Filters are the same, skipping load")
            return
        }
        _state.update { it.copy(shouldShowFiltersSheet = false, content = State.Content.Loading) }
        loadHistory()
    }

    fun onTimeFilterSelected(timeFilterItem: State.MonthFilter) {
        val existingIndex = _state.value.historyItems?.indexOfFirst {
            it.dateTime?.isAfter(timeFilterItem.start) == true &&
                it.dateTime?.isBefore(timeFilterItem.end) == true
        }
        if (existingIndex == null || existingIndex == -1) {
            viewModelScope.launch {
                if (_state.value.canLoadMore.not()) {
                    // if we don't have the index in range and we loaded all data, we scroll to the first item before the selected date
                    val firstIndexBefore = _state.value.historyItems?.indexOfFirst {
                        it.dateTime?.isBefore(timeFilterItem.end) == true
                    }
                    if (firstIndexBefore != null && firstIndexBefore >= 0) {
                        blockScrollHandlingAndExecute {
                            sendEvent(HistoryEvent.ScrollToItem(firstIndexBefore))
                            selectDate(_state.value.historyItems?.getOrNull(firstIndexBefore)?.dateTime)
                        }
                        return@launch
                    }
                }
                // we don't have data, we need to load new range
                _state.update { it.copy(loadMoreState = State.LoadingMoreState.NewRange, areScrollEventsIgnored = true) }
                getAccountHistoryUseCase.getHistoryChunk(
                    args.accountAddress,
                    _state.value.filters.copy(start = timeFilterItem.end)
                ).onSuccess { historyData ->
                    updateStateWith(historyData, false)
                    // after data load we scroll to first item before the selected date
                    val scrollTo = _state.value.historyItems?.indexOfFirst {
                        it.dateTime?.isBefore(timeFilterItem.end) == true
                    }
                    if (scrollTo != null && scrollTo != -1) {
                        delay(SCROLL_DELAY_AFTER_LOAD)
                        blockScrollHandlingAndExecute {
                            sendEvent(HistoryEvent.ScrollToItem(scrollTo))
                            selectDate(_state.value.historyItems?.getOrNull(scrollTo)?.dateTime)
                        }
                    }
                    _state.update { it.copy(loadMoreState = null, areScrollEventsIgnored = false) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            loadMoreState = null,
                            areScrollEventsIgnored = false,
                            uiMessage = UiMessage.ErrorMessage(error = error)
                        )
                    }
                }
            }
        } else {
            // if we have the index in range, we just scroll to it
            viewModelScope.launch {
                blockScrollHandlingAndExecute {
                    sendEvent(HistoryEvent.ScrollToItem(existingIndex))
                    selectDate(_state.value.historyItems?.getOrNull(existingIndex)?.dateTime)
                }
            }
        }
    }

    private suspend fun blockScrollHandlingAndExecute(block: suspend () -> Unit) {
        _state.update { it.copy(areScrollEventsIgnored = true) }
        block()
        delay(SCROLL_DELAY_AFTER_LOAD)
        _state.update { it.copy(areScrollEventsIgnored = false) }
    }

    fun onOpenTransactionDetails(txId: String) {
        viewModelScope.launch {
            state.value.accountWithAssets?.let { account ->
                val dashboardUrl = NetworkId.from(account.account.networkID).dashboardUrl()
                sendEvent(HistoryEvent.OnTransactionItemClick("$dashboardUrl/transaction/$txId/summary"))
            }
        }
    }

    fun onLoadMore(direction: ScrollInfo.Direction) {
        when (direction) {
            ScrollInfo.Direction.UP -> {
                if (_state.value.canLoadMoreUp.not()) {
                    Timber.d("History: Nothing to load at the top")
                    return
                }
                _state.update { it.copy(loadMoreState = State.LoadingMoreState.Up) }
                viewModelScope.launch {
                    _state.value.historyData?.let { currentData ->
                        getAccountHistoryUseCase.loadMore(args.accountAddress, currentData, prepend = true)
                            .onSuccess { historyData ->
                                updateStateWith(historyData)
                            }.onFailure { error ->
                                _state.update { it.copy(loadMoreState = null, uiMessage = UiMessage.ErrorMessage(error)) }
                            }
                    }
                }
            }

            ScrollInfo.Direction.DOWN -> {
                if (_state.value.canLoadMoreDown.not()) {
                    Timber.d("History: Nothing to load at the bottom")
                    return
                }
                _state.update { it.copy(loadMoreState = State.LoadingMoreState.Down) }
                viewModelScope.launch {
                    _state.value.historyData?.let { currentData ->
                        getAccountHistoryUseCase.loadMore(args.accountAddress, currentData)
                            .onSuccess { historyData ->
                                updateStateWith(historyData)
                            }.onFailure { error ->
                                _state.update { it.copy(loadMoreState = null, uiMessage = UiMessage.ErrorMessage(error)) }
                            }
                    }
                }
            }
        }
    }

    fun onScrollEvent(event: ScrollInfo) {
        if (_state.value.areScrollEventsIgnored.not()) {
            val items = _state.value.historyItems ?: return
            val firstVisibleItemDate = event.firstVisibleIndex?.let { items.getOrNull(it) }?.dateTime ?: return
            val lastVisibleItemDate = event.lastVisibleIndex?.let { items.getOrNull(it) }?.dateTime ?: return
            _state.update { it.copy(firstVisibleIndex = event.firstVisibleIndex) }
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

    fun onResourceFilterSelected(resource: Resource?) {
        _state.update { state ->
            val updatedFilters = state.filters.copy(
                resource = resource
            )
            state.copy(filters = updatedFilters)
        }
    }

    private fun selectDate(date: ZonedDateTime?) {
        val selectedDate = date ?: return
        var scrollToIndex = -1
        _state.update { state ->
            val toUpdate = state.timeFilterItems.firstOrNull { selectedDate.isBefore(it.data.end) || it.data.end.isEqual(date) }
                ?: return@update state
            val updateItems = state.timeFilterItems.mapIndexed { index, filter ->
                if (filter.data == toUpdate.data) {
                    scrollToIndex = index
                    filter.copy(selected = true)
                } else {
                    filter.copy(selected = false)
                }
            }.toPersistentList()
            state.copy(
                timeFilterItems = updateItems
            )
        }
        if (scrollToIndex == -1) return
        viewModelScope.launch {
            sendEvent(HistoryEvent.ScrollToTimeFilter(scrollToIndex))
        }
    }

    private fun updateStateWith(historyData: TransactionHistoryData, resetLoadingMoreState: Boolean = true) {
        _state.update {
            val historyItems = mutableListOf<HistoryItem>()
            historyData.groupedByDate.forEach { (_, transactionItems) ->
                val firstTx = transactionItems.first()
                val date = firstTx.timestamp?.atZone(ZonedDateTime.now().zone) ?: ZonedDateTime.now()
                historyItems.add(HistoryItem.Date(date))
                transactionItems.forEach { historyItems.add(HistoryItem.Transaction(it)) }
            }
            it.copy(
                content = if (historyItems.isEmpty()) {
                    State.Content.Empty
                } else {
                    State.Content.Loaded(
                        historyData = historyData,
                        historyItems = historyItems.toPersistentList(),
                    )
                },
                loadMoreState = if (resetLoadingMoreState) null else it.loadMoreState,
                isRefreshing = false
            )
        }
    }

    companion object {
        private const val SCROLL_DELAY_AFTER_LOAD = 300L
    }
}

internal sealed interface HistoryEvent : OneOffEvent {
    data class OnTransactionItemClick(val url: String) : HistoryEvent
    data class ScrollToItem(val index: Int, val loadingOffset: Boolean = false) : HistoryEvent
    data class ScrollToTimeFilter(val index: Int) : HistoryEvent
}

data class State(
    val accountWithAssets: AccountWithAssets? = null,
    val content: Content = Content.Loading,
    val uiMessage: UiMessage? = null,
    val shouldShowFiltersSheet: Boolean = false,
    val filters: HistoryFilters = HistoryFilters(),
    val timeFilterItems: ImmutableList<Selectable<MonthFilter>> = persistentListOf(),
    val loadMoreState: LoadingMoreState? = null,
    val firstVisibleIndex: Int? = null,
    val areScrollEventsIgnored: Boolean = false,
    val isRefreshing: Boolean = false
) : UiState {

    val historyData
        get() = when (content) {
            is Content.Loaded -> content.historyData
            else -> null
        }

    val shouldShowFiltersButton: Boolean
        get() = content is Content.Loaded || filters.isAnyFilterSet

    val shouldEnableUserInteraction: Boolean
        get() = content !is Content.Loading && loadMoreState == null

    val historyItems
        get() = when (content) {
            is Content.Loaded -> content.historyItems
            else -> null
        }

    private val currentFilters: HistoryFilters?
        get() = when (content) {
            is Content.Loaded -> content.historyData.filters
            else -> null
        }

    // we use this to show  for now we filter out LSU -
    val fungibleResourcesUsedInFilters: List<Resource.FungibleResource>
        get() = accountWithAssets?.assets?.tokens?.map {
            it.resource
        }.orEmpty() + accountWithAssets?.assets?.poolUnits?.map {
            it.stake
        }.orEmpty()

    val nonFungibleResourcesUsedInFilters: List<Resource.NonFungibleResource>
        get() = accountWithAssets?.assets?.nonFungibles?.map { it.collection }.orEmpty()

    val canLoadMoreUp: Boolean
        get() = content is Content.Loaded && loadMoreState == null && content.historyData.prevCursorId != null

    val canLoadMoreDown: Boolean
        get() = content is Content.Loaded && loadMoreState == null && content.historyData.nextCursorId != null

    val canLoadMore: Boolean
        get() = canLoadMoreUp || canLoadMoreDown

    val filtersChanged: Boolean
        get() = filters != currentFilters

    sealed class Content {
        data object Loading : Content()
        data object Empty : Content()
        data class Loaded(
            val historyData: TransactionHistoryData,
            val historyItems: ImmutableList<HistoryItem>
        ) : Content()
    }

    enum class LoadingMoreState {
        Up, Down, NewRange
    }

    data class MonthFilter(val month: String, val start: ZonedDateTime, val end: ZonedDateTime)
}

sealed interface HistoryItem {

    val dateTime: ZonedDateTime?
    val key: String

    data class Date(val item: ZonedDateTime) : HistoryItem {
        override val dateTime: ZonedDateTime
            get() = item

        override val key: String = item.year.toString() + item.dayOfYear
    }

    data class Transaction(val transactionItem: TransactionHistoryItem) : HistoryItem {
        override val dateTime: ZonedDateTime?
            get() = transactionItem.timestamp?.atZone(ZoneId.systemDefault())

        override val key: String = transactionItem.txId
    }
}
