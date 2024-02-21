@file:OptIn(ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.history.composables.FiltersDialog
import com.babylon.wallet.android.presentation.history.composables.FiltersStrip
import com.babylon.wallet.android.presentation.history.composables.HistoryTransactionItem
import com.babylon.wallet.android.presentation.history.composables.TypeAndTimestampLabel
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.utils.dayMonthDateFormat
import com.babylon.wallet.android.utils.openUrl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    HistoryContent(
        modifier = modifier.fillMaxSize(),
        onBackClick = onBackClick,
        state = state,
        onTimeFilterSelected = viewModel::onTimeFilterSelected,
        onShowFilters = viewModel::onShowFiltersDialog,
        onClearAllFilters = viewModel::onClearAllFilters,
        onShowResults = viewModel::onShowResults,
        onOpenTransactionDetails = viewModel::onOpenTransactionDetails,
        onLoadMore = viewModel::onLoadMore,
        onScrollEvent = viewModel::onScrollEvent,
        onTransactionTypeFilterSelected = viewModel::onTransactionTypeFilterSelected,
        onTransactionClassFilterSelected = viewModel::onTransactionClassFilterSelected,
        onResourceFilterSelected = viewModel::onResourceFilterSelected,
        onSubmittedByFilterChanged = viewModel::onSubmittedByFilterChanged,
        listState = listState
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is HistoryEvent.OnTransactionItemClick -> context.openUrl(event.url)
                is HistoryEvent.ScrollToItem -> listState.scrollToItem(event.index + 2)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: State,
    onTimeFilterSelected: (TimeFilterItem.Month) -> Unit,
    onShowFilters: (Boolean) -> Unit,
    onClearAllFilters: () -> Unit,
    onShowResults: () -> Unit,
    onOpenTransactionDetails: (String) -> Unit,
    onLoadMore: () -> Unit,
    onScrollEvent: (ScrollInfo) -> Unit,
    onTransactionTypeFilterSelected: (HistoryFilters.TransactionType?) -> Unit,
    onTransactionClassFilterSelected: (TransactionClass?) -> Unit,
    onResourceFilterSelected: (Resource) -> Unit,
    onSubmittedByFilterChanged: (HistoryFilters.SubmittedBy?) -> Unit,
    listState: LazyListState
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    SyncSheetState(sheetState = bottomSheetState, isSheetVisible = state.showFiltersSheet, onSheetClosed = { onShowFilters(false) })
    val timeFilterState = rememberLazyListState()
    LaunchedEffect(state.timeFilterItems) {
        if (state.timeFilterItems.isNotEmpty()) {
            timeFilterState.scrollToItem(state.timeFilterItems.lastIndex)
        }
    }
    MonitorListScroll(state = listState, fixedListElements = 2, onLoadMore = {
        if (state.isRefreshing) return@MonitorListScroll
        onLoadMore()
    }, onScrollEvent = {
        onScrollEvent(it)
    })
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            RadixCenteredTopAppBar(
                title = stringResource(id = R.string.history_title),
                onBackClick = onBackClick,
                backIconType = BackIconType.Close,
                windowInsets = WindowInsets.statusBars,
                containerColor = RadixTheme.colors.defaultBackground,
                actions = {
                    IconButton(onClick = { onShowFilters(true) }) {
                        Icon(
                            painterResource(
                                id = DSR.ic_filter_list
                            ),
                            tint = Color.Unspecified,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            state = listState
        ) {
            state.accountWithAssets?.let {
                item {
                    SimpleAccountCard(
                        account = it.account,
                        modifier = Modifier
                            .background(RadixTheme.colors.defaultBackground)
                            .fillMaxWidth()
                            .padding(
                                horizontal = RadixTheme.dimensions.paddingMedium,
                                vertical = RadixTheme.dimensions.paddingSmall
                            )
                    )
                }
            }
            stickyHeader {
                if (state.filters.isAnyFilterSet) {
                    FiltersStrip(
                        historyFilters = state.filters,
                        userInteractionEnabled = state.content !is Content.Loading && state.isLoadingMore.not(),
                        onTransactionTypeFilterRemoved = {
                            onTransactionTypeFilterSelected(null)
                            onShowResults()
                        },
                        onTransactionClassFilterRemoved = {
                            onTransactionClassFilterSelected(null)
                            onShowResults()
                        },
                        onResourceFilterSelected = onResourceFilterSelected,
                        onSubmittedByFilterRemoved = {
                            onSubmittedByFilterChanged(null)
                            onShowResults()
                        }
                    )
                }
                if (state.timeFilterItems.isNotEmpty()) {
                    TimePicker(
                        timeFilterState = timeFilterState,
                        timeFilterItems = state.timeFilterItems,
                        onTimeFilterSelected = onTimeFilterSelected
                    )
                }
            }
            when (val content = state.content) {
                Content.Empty -> {
                    item {
                        EmptyContent()
                    }
                }

                is Content.Loaded -> {
                    items(content.historyItems) { item ->
                        when (item) {
                            is HistoryItem.Date -> {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(RadixTheme.colors.gray5)
                                        .padding(RadixTheme.dimensions.paddingMedium),
                                    text = item.item.toInstant().dayMonthDateFormat(),
                                    style = RadixTheme.typography.body2Header,
                                    color = RadixTheme.colors.gray2
                                )
                            }

                            is HistoryItem.Transaction -> {
                                when (item.transactionItem.transactionClass) {
                                    TransactionClass.AccountDespositSettingsUpdate -> {
                                        AccountDepositSettingsUpdateItem(
                                            item.transactionItem,
                                            modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                                            onClick = {
                                                onOpenTransactionDetails(item.transactionItem.txId)
                                            }
                                        )
                                    }

                                    else -> {
                                        if (item.transactionItem.unknownTransaction) {
                                            UnknownTransactionItem(
                                                item.transactionItem,
                                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium)
                                            ) {
                                                onOpenTransactionDetails(item.transactionItem.txId)
                                            }
                                        } else {
                                            HistoryTransactionItem(
                                                modifier = Modifier.padding(horizontal = RadixTheme.dimensions.paddingMedium),
                                                transactionItem = item.transactionItem,
                                                onClick = {
                                                    onOpenTransactionDetails(item.transactionItem.txId)
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                            }
                        }
                    }
                }

                Content.Loading -> {
                    repeat(4) {
                        item {
                            LoadingItemPlaceholder(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingMedium))
                        }
                    }
                }
            }
            if (state.isLoadingMore) {
                item {
                    LoadingItemPlaceholder(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingMedium))
                }
            }
        }
    }

    if (state.showFiltersSheet) {
        DefaultModalSheetLayout(modifier = Modifier.imePadding(), sheetState = bottomSheetState, sheetContent = {
            FiltersDialog(
                state = state,
                onDismiss = {
                    onShowFilters(false)
                    onShowResults()
                },
                onClearAllFilters = onClearAllFilters,
                onTransactionTypeFilterSelected = onTransactionTypeFilterSelected,
                onTransactionClassFilterSelected = onTransactionClassFilterSelected,
                onResourceFilterSelected = onResourceFilterSelected,
                onSubmittedByFilterChanged = onSubmittedByFilterChanged
            )
        }, showDragHandle = true, containerColor = RadixTheme.colors.defaultBackground, onDismissRequest = {
            onShowFilters(false)
            onShowResults()
        })
    }
}

@Composable
private fun LoadingItemPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(horizontal = RadixTheme.dimensions.paddingMedium)
            .height(150.dp)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .radixPlaceholder(visible = true)
    )
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Image(
            modifier = Modifier.height(130.dp),
            painter = painterResource(id = R.drawable.ic_empty_fungibles),
            contentDescription = null
        )

        Text(
            text = "There are not transactions for this account", // TODO crowdin
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun TimePicker(
    modifier: Modifier = Modifier,
    timeFilterState: LazyListState,
    timeFilterItems: ImmutableList<Selectable<TimeFilterItem>>,
    onTimeFilterSelected: (TimeFilterItem.Month) -> Unit
) {
    LazyRow(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium),
        state = timeFilterState,
        contentPadding = PaddingValues(RadixTheme.dimensions.paddingMedium),
        content = {
            items(timeFilterItems) { item ->
                val text = when (val data = item.data) {
                    is TimeFilterItem.Month -> data.month
                    is TimeFilterItem.Year -> data.year.toString()
                }
                Text(
                    modifier = Modifier
                        .clip(RadixTheme.shapes.circle)
                        .applyIf(
                            item.data is TimeFilterItem.Month,
                            Modifier.clickable {
                                item.data
                                onTimeFilterSelected(item.data as TimeFilterItem.Month)
                            }
                        )
                        .applyIf(
                            item.selected,
                            Modifier.background(RadixTheme.colors.gray4, RadixTheme.shapes.circle)
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingSmall
                        ),
                    text = text,
                    style = RadixTheme.typography.body2HighImportance,
                    maxLines = 1,
                    color = if (item.selected) RadixTheme.colors.gray1 else RadixTheme.colors.gray2,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
private fun UnknownTransactionItem(item: TransactionHistoryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingMedium),
            horizontalArrangement = Arrangement.End
        ) {
            TypeAndTimestampLabel(item = item)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(1.dp)
                .background(RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectBottomMedium)
                .padding(RadixTheme.dimensions.paddingMedium),
            text = "This transaction cannot be summarized. Only the raw transaction manifest may be viewed.", // TODO crowding
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun AccountDepositSettingsUpdateItem(item: TransactionHistoryItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(color = RadixTheme.colors.defaultBackground, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(RadixTheme.dimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
        ) {
            Icon(painter = painterResource(id = DSR.ic_tx_account_settings), contentDescription = null, tint = Color.Unspecified)
            Text(
                modifier = Modifier.weight(1f),
                text = "Settings", // TODO crowdin
                style = RadixTheme.typography.body2Header,
                color = RadixTheme.colors.gray1
            )
            TypeAndTimestampLabel(item = item)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectMedium)
                .padding(RadixTheme.dimensions.paddingMedium),
            text = "Updated Account Deposit Settings", // TODO crowding
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSheetState(
    sheetState: SheetState,
    isSheetVisible: Boolean,
    onSheetClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = isSheetVisible) {
        onSheetClosed()
    }

    LaunchedEffect(isSheetVisible) {
        if (isSheetVisible) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            onSheetClosed()
        }
    }
}

@Composable
private fun MonitorListScroll(state: LazyListState, fixedListElements: Int, onScrollEvent: (ScrollInfo) -> Unit, onLoadMore: () -> Unit) {
    var previousIndex by remember(state) { mutableIntStateOf(state.firstVisibleItemIndex) }
    var previousScrollOffset by remember(state) { mutableIntStateOf(state.firstVisibleItemScrollOffset) }
    val scrollingUp = remember(state) {
        derivedStateOf {
            if (previousIndex != state.firstVisibleItemIndex) {
                previousIndex > state.firstVisibleItemIndex
            } else {
                previousScrollOffset >= state.firstVisibleItemScrollOffset
            }.also {
                previousIndex = state.firstVisibleItemIndex
                previousScrollOffset = state.firstVisibleItemScrollOffset
            }
        }
    }.value
    val loadMore by remember(state) {
        derivedStateOf {
            val lastItem = state.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            state.layoutInfo.totalItemsCount > fixedListElements && lastItem.index == state.layoutInfo.totalItemsCount - 1
        }
    }
    val scrollInfo by remember(state) {
        derivedStateOf {
            val firstItem = state.layoutInfo.visibleItemsInfo.firstOrNull {
                if (fixedListElements == 0) {
                    true
                } else {
                    it.index > fixedListElements - 1
                }
            }
            val lastItem = state.layoutInfo.visibleItemsInfo.lastOrNull()
            ScrollInfo(
                fixedListElements = fixedListElements,
                firstVisibleIndex = firstItem?.index,
                lastVisibleIndex = lastItem?.index,
                totalCount = state.layoutInfo.totalItemsCount
            )
        }
    }
    LaunchedEffect(scrollInfo, scrollingUp) {
        snapshotFlow {
            scrollInfo.copy(
                direction = if (scrollingUp) ScrollInfo.Direction.UP else ScrollInfo.Direction.DOWN
            )
        }.collect { event ->
            onScrollEvent(event)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore }.filter { it }.collect {
            onLoadMore()
        }
    }
}

data class ScrollInfo(
    val fixedListElements: Int = 0,
    val direction: Direction? = null,
    private val firstVisibleIndex: Int?,
    private val lastVisibleIndex: Int?,
    val totalCount: Int
) {

    val firstVisible: Int?
        get() = firstVisibleIndex?.let { it - fixedListElements }

    val lastVisible: Int?
        get() = lastVisibleIndex?.let { it - fixedListElements }

    enum class Direction {
        UP, DOWN
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryContentPreview() {
    RadixWalletTheme {
        HistoryContent(
            modifier = Modifier
                .padding(10.dp)
                .background(color = Color.Gray),
            onBackClick = {},
            state = State(
                accountWithAssets = AccountWithAssets(
                    SampleDataProvider().sampleAccount(
                        address = "rdx_t_12382918379821",
                        name = "Savings account"
                    )
                ),
                content = Content.Loading,
            ),
            onTimeFilterSelected = {},
            onShowFilters = {},
            onClearAllFilters = {},
            onShowResults = {},
            onOpenTransactionDetails = {},
            onLoadMore = {},
            onScrollEvent = {},
            onTransactionTypeFilterSelected = {},
            onTransactionClassFilterSelected = {},
            onResourceFilterSelected = {},
            onSubmittedByFilterChanged = {},
            listState = rememberLazyListState()
        )
    }
}
