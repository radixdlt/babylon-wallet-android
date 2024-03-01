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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
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
                    if (accountWithAssets.details?.firstTransactionDate == null) {
                        getAccountFirstTransactionDateUseCase(args.accountAddress)
                    } else {
                        computeTimeFilters(accountWithAssets.details.firstTransactionDate)
                    }
                }
            }
        }
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            getAccountHistoryUseCase.getHistory(args.accountAddress, _state.value.filters).onSuccess { historyData ->
                updateStateWith(historyData)
            }.onFailure {}
        }
    }

    @Suppress("MagicNumber")
    private fun computeTimeFilters(firstTransaction: Instant) {
        val result = mutableListOf<MonthFilter>()
        val now = ZonedDateTime.now()
        var firstTransactionDate = ZonedDateTime.ofInstant(firstTransaction, now.zone)
        while (firstTransactionDate.isBefore(now)) {
            val temp = firstTransactionDate.with(TemporalAdjusters.lastDayOfMonth())
            var anchor = ZonedDateTime.of(temp.year, temp.monthValue, temp.dayOfMonth, 23, 59, 59, 999, now.zone)
            if (anchor.isAfter(now)) anchor = now
            result.add(
                MonthFilter(
                    anchor.toMonthString().uppercase(),
                    anchor
                )
            )
            firstTransactionDate = firstTransactionDate.plusMonths(1)
        }
        _state.update { state ->
            state.copy(
                timeFilterItems = result.mapIndexed { index, monthFilter ->
                    Selectable(monthFilter, index == result.lastIndex)
                }.toPersistentList()
            )
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
            it.copy(filters = HistoryFilters(start = it.filters.start))
        }
    }

    fun onShowResults() {
        if (_state.value.filtersChanged.not()) {
            Timber.d("History: Filters are the same, skipping load")
            return
        }
        _state.update { it.copy(showFiltersSheet = false, content = Content.Loading) }
        loadHistory()
    }

    fun onTimeFilterSelected(timeFilterItem: MonthFilter) {
        val existingIndex = _state.value.historyItems?.indexOfFirst {
            it is HistoryItem.Transaction && it.dateTime?.isBefore(timeFilterItem.date) == true
        }
        val item = existingIndex?.let { _state.value.historyItems?.getOrNull(it) }
        if (existingIndex == null || existingIndex == -1) {
            _state.update { it.copy(loadMoreState = LoadingMoreState.NewRange) }
            viewModelScope.launch {
                getAccountHistoryUseCase.getHistoryChunk(
                    args.accountAddress,
                    _state.value.filters.copy(start = timeFilterItem.date)
                ).onSuccess { historyData ->
                    updateStateWith(historyData, false)
                    val scrollTo = _state.value.historyItems?.indexOfFirst {
                        it.dateTime?.isBefore(timeFilterItem.date) == true
                    }
                    if (scrollTo != null && scrollTo != -1) {
                        delay(SCROLL_DELAY_AFTER_LOAD)
                        sendEvent(HistoryEvent.ScrollToItem(scrollTo))
                    }
                    _state.update { it.copy(loadMoreState = null) }
                }
            }
        } else {
            viewModelScope.launch {
                blockScrollHandlingAndExecute {
                    sendEvent(HistoryEvent.ScrollToItem(existingIndex))
                    selectDate(item?.dateTime)
                }
            }
        }
    }

    private suspend fun blockScrollHandlingAndExecute(block: suspend () -> Unit) {
        _state.update { it.copy(ignoreScrollEvents = true) }
        block()
        delay(SCROLL_DELAY_AFTER_LOAD)
        _state.update { it.copy(ignoreScrollEvents = false) }
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
                if (_state.value.canLoadMoreUp.not() || _state.value.loadMoreState != null) {
                    Timber.d("History: Nothing to load at the top")
                    return
                }
                Timber.d("History: Loading data at the top, cursor: ${_state.value.historyData?.prevCursorId}")
                _state.update { it.copy(loadMoreState = LoadingMoreState.Up) }
                val currentFirstTxId =
                    _state.value.historyItems?.filterIsInstance<HistoryItem.Transaction>()?.firstOrNull()?.transactionItem?.txId
                viewModelScope.launch {
                    _state.value.historyData?.let { currentData ->
                        getAccountHistoryUseCase.loadMore(args.accountAddress, currentData, prepend = true)
                            .onSuccess { historyData ->
                                if (_state.value.firstVisibleIndex == 0) {
                                    Timber.d("History: Will execute scroll to item after prepend")
                                    updateStateWith(historyData, resetLoadingMoreState = false)
                                    val scrollTo = _state.value.historyItems?.indexOfFirst {
                                        it.key == currentFirstTxId
                                    }
                                    if (scrollTo != null && scrollTo != -1) {
                                        delay(SCROLL_DELAY_AFTER_LOAD) // delay so the list is updated
                                        sendEvent(HistoryEvent.ScrollToItem(scrollTo))
                                    }
                                }
                                updateStateWith(historyData.copy(lastPrependedIds = emptySet()))
                            }
                    }
                }
            }

            ScrollInfo.Direction.DOWN -> {
                if (_state.value.canLoadMoreDown.not() || _state.value.loadMoreState != null) {
                    return
                }
                _state.update { it.copy(loadMoreState = LoadingMoreState.Down) }
                viewModelScope.launch {
                    _state.value.historyData?.let { currentData ->
                        getAccountHistoryUseCase.loadMore(args.accountAddress, currentData)
                            .onSuccess { historyData ->
                                updateStateWith(historyData)
                            }
                    }
                }
            }
        }
    }

    fun onScrollEvent(event: ScrollInfo) {
        if (_state.value.ignoreScrollEvents.not()) {
            val items = _state.value.historyItems ?: return
            val firstVisibleItemDate = event.firstVisible?.let { items.getOrNull(it) }?.dateTime ?: return
            val lastVisibleItemDate = event.lastVisible?.let { items.getOrNull(it) }?.dateTime ?: return
            _state.update { it.copy(firstVisibleIndex = event.firstVisible, lastVisibleIndex = event.lastVisible) }
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
            val toUpdate = state.timeFilterItems.firstOrNull { selectedDate.isBefore(it.data.date) || it.data.date.isEqual(date) }
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
                historyItems.add(HistoryItem.Date(date, historyData.lastPrependedIds.contains(firstTx.txId)))
                transactionItems.forEach { historyItems.add(HistoryItem.Transaction(it, historyData.lastPrependedIds.contains(it.txId))) }
            }
            it.copy(
                content = if (historyItems.isEmpty()) {
                    Content.Empty
                } else {
                    Content.Loaded(
                        historyData = historyData,
                        historyItems = historyItems.toPersistentList()
                    )
                },
                loadMoreState = if (resetLoadingMoreState) null else it.loadMoreState
            )
        }
    }

    companion object {
        private const val SCROLL_DELAY_AFTER_LOAD = 300L
    }
}

internal sealed interface HistoryEvent : OneOffEvent {
    data class OnTransactionItemClick(val url: String) : HistoryEvent
    data class ScrollToItem(val index: Int) : HistoryEvent
    data class ScrollToTimeFilter(val index: Int) : HistoryEvent
}

data class State(
    val accountWithAssets: AccountWithAssets? = null,
    val content: Content = Content.Loading,
    val uiMessage: UiMessage? = null,
    val showFiltersSheet: Boolean = false,
    val filters: HistoryFilters = HistoryFilters(),
    val timeFilterItems: ImmutableList<Selectable<MonthFilter>> = persistentListOf(),
    val loadMoreState: LoadingMoreState? = null,
    val firstVisibleIndex: Int? = null,
    val lastVisibleIndex: Int? = null,
    val ignoreScrollEvents: Boolean = false
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

    val fungibleResources: List<Resource.FungibleResource>
        get() = accountWithAssets?.assets?.knownFungibles.orEmpty()

    val nonFungibleResources: List<Resource.NonFungibleResource>
        get() = accountWithAssets?.assets?.knownNonFungibles.orEmpty()

    val canLoadMoreUp: Boolean
        get() = content is Content.Loaded && content.historyData.prevCursorId != null

    val canLoadMoreDown: Boolean
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

enum class LoadingMoreState {
    Up, Down, NewRange
}

data class MonthFilter(val month: String, val date: ZonedDateTime)

sealed interface HistoryItem {

    val dateTime: ZonedDateTime?
    val key: String

    data class Date(val item: ZonedDateTime, val showAsPlaceholder: Boolean = false) : HistoryItem {
        override val dateTime: ZonedDateTime
            get() = item

        override val key: String = item.year.toString() + item.dayOfYear
    }

    data class Transaction(val transactionItem: TransactionHistoryItem, val showAsPlaceholder: Boolean = false) : HistoryItem {
        override val dateTime: ZonedDateTime?
            get() = transactionItem.timestamp?.atZone(ZoneId.systemDefault())

        override val key: String = transactionItem.txId
    }
}
