package com.babylon.wallet.android.presentation.account.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.account.composable.FiltersDialog
import com.babylon.wallet.android.presentation.account.composable.FiltersStrip
import com.babylon.wallet.android.presentation.account.composable.TransactionHistoryItem
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.DefaultPullToRefreshContainer
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.utils.dayMonthDateFull
import com.babylon.wallet.android.utils.openUrl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

private const val FIXED_LIST_ELEMENTS = 2

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val timeFilterScrollState = rememberLazyListState()
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
        listState = listState,
        timeFilterScrollState = timeFilterScrollState,
        onMessageShown = viewModel::onMessageShown,
        onRefresh = viewModel::onRefresh
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is HistoryEvent.OnTransactionItemClick -> context.openUrl(event.url)
                is HistoryEvent.ScrollToItem -> {
                    listState.scrollToItem(event.index)
                }

                is HistoryEvent.ScrollToTimeFilter -> timeFilterScrollState.scrollToItem(event.index)
            }
        }
    }
}

private class AccountHeaderCardNestedScrollConnection(
    private val lazyListState: LazyListState,
    private val accountCardHeight: Int,
) : NestedScrollConnection {

    var cardOffset by mutableIntStateOf(0)
        private set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (available.y < 0 || lazyListState.firstVisibleItemIndex < 2) {
            val delta = available.y.toInt()
            val newOffset = cardOffset + delta
            val previousOffset = cardOffset
            cardOffset = newOffset.coerceIn(-accountCardHeight, 0)
            val consumed = cardOffset - previousOffset
            Offset(0f, consumed.toFloat())
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (available.y > 1f) {
            AnimationState(
                initialValue = cardOffset.toFloat(),
            ).animateTo(0f) {
                cardOffset = value.toInt()
            }
        }
        return super.onPostFling(consumed, available)
    }
}

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    state: State,
    onTimeFilterSelected: (State.MonthFilter) -> Unit,
    onShowFilters: (Boolean) -> Unit,
    onClearAllFilters: () -> Unit,
    onShowResults: () -> Unit,
    onOpenTransactionDetails: (String) -> Unit,
    onLoadMore: (ScrollInfo.Direction) -> Unit,
    onScrollEvent: (ScrollInfo) -> Unit,
    onTransactionTypeFilterSelected: (HistoryFilters.TransactionType?) -> Unit,
    onTransactionClassFilterSelected: (TransactionClass?) -> Unit,
    onResourceFilterSelected: (Resource?) -> Unit,
    listState: LazyListState,
    timeFilterScrollState: LazyListState,
    onMessageShown: () -> Unit,
    onRefresh: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    SyncSheetState(sheetState = bottomSheetState, isSheetVisible = state.shouldShowFiltersSheet, onSheetClosed = { onShowFilters(false) })
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    LaunchedEffect(state.timeFilterItems.size) {
        if (state.timeFilterItems.isNotEmpty()) {
            timeFilterScrollState.scrollToItem(state.timeFilterItems.lastIndex)
        }
    }
    MonitorListScroll(state = listState, fixedListElements = FIXED_LIST_ELEMENTS, onLoadMore = { direction ->
        when (direction) {
            ScrollInfo.Direction.UP -> {
                if (state.canLoadMoreUp) {
                    onLoadMore(ScrollInfo.Direction.UP)
                }
            }

            ScrollInfo.Direction.DOWN -> {
                if (state.canLoadMoreDown) {
                    onLoadMore(ScrollInfo.Direction.DOWN)
                }
            }
        }
    }, onScrollEvent = {
        onScrollEvent(it)
    })
    DefaultPullToRefreshContainer(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
        Scaffold(
            modifier = modifier.imePadding(),
            topBar = {
                RadixCenteredTopAppBar(
                    title = stringResource(id = R.string.transactionHistory_title),
                    onBackClick = onBackClick,
                    backIconType = BackIconType.Close,
                    windowInsets = WindowInsets.statusBars,
                    containerColor = RadixTheme.colors.defaultBackground,
                    actions = {
                        AnimatedVisibility(visible = state.shouldShowFiltersButton, enter = fadeIn(), exit = fadeOut()) {
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
                    }
                )
            },
            snackbarHost = {
                RadixSnackbarHost(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    hostState = snackBarHostState
                )
            },
            containerColor = RadixTheme.colors.gray5
        ) { padding ->
            var accountCardHeight by rememberSaveable {
                mutableIntStateOf(0)
            }
            val connection = remember(accountCardHeight) {
                AccountHeaderCardNestedScrollConnection(listState, accountCardHeight)
            }
            var topSectionHeight by rememberSaveable {
                mutableIntStateOf(0)
            }
            val density = LocalDensity.current
            val spacerHeight by remember(topSectionHeight, density) {
                derivedStateOf {
                    with(density) { (topSectionHeight + connection.cardOffset).toDp() }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .nestedScroll(connection)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(spacerHeight))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth(),
                        state = listState,
                        userScrollEnabled = state.loadMoreState != State.LoadingMoreState.NewRange
                    ) {
                        when (val content = state.content) {
                            State.Content.Empty -> {
                                item {
                                    EmptyContent()
                                }
                            }

                            is State.Content.Loaded -> {
                                itemsIndexed(content.historyItems, key = { _, item -> item.key }) { index, item ->
                                    if (index == 0 && state.loadMoreState == State.LoadingMoreState.Up) {
                                        LoadingItemPlaceholder(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingMedium))
                                    }
                                    when (item) {
                                        is HistoryItem.Date -> {
                                            Text(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(RadixTheme.colors.gray5)
                                                    .padding(RadixTheme.dimensions.paddingMedium)
                                                    .radixPlaceholder(
                                                        visible = state.loadMoreState == State.LoadingMoreState.NewRange
                                                    ),
                                                text = item.item.toInstant().dayMonthDateFull(),
                                                style = RadixTheme.typography.body2Header,
                                                color = RadixTheme.colors.gray2
                                            )
                                        }

                                        is HistoryItem.Transaction -> {
                                            TransactionHistoryItem(
                                                modifier = Modifier
                                                    .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                                                    .radixPlaceholder(
                                                        visible = state.loadMoreState == State.LoadingMoreState.NewRange
                                                    )
                                                    .shadow(elevation = 6.dp, shape = RadixTheme.shapes.roundedRectMedium),
                                                transactionItem = item.transactionItem,
                                                onClick = {
                                                    onOpenTransactionDetails(item.transactionItem.txId)
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                                        }
                                    }
                                }
                            }

                            State.Content.Loading -> {
                                repeat(4) {
                                    item(key = "loader$it") {
                                        LoadingItemPlaceholder(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingMedium))
                                    }
                                }
                            }
                        }
                        if (state.loadMoreState == State.LoadingMoreState.Down) {
                            item(key = "loaderDown") {
                                LoadingItemPlaceholder(modifier = Modifier.padding(vertical = RadixTheme.dimensions.paddingMedium))
                            }
                        }
                    }
                }
                Column(
                    Modifier
                        .offset { IntOffset(0, connection.cardOffset) }
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            topSectionHeight = size.height
                        }
                ) {
                    state.accountWithAssets?.let {
                        SimpleAccountCard(
                            account = it.account,
                            modifier = Modifier
                                .background(RadixTheme.colors.defaultBackground)
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .padding(
                                    start = RadixTheme.dimensions.paddingMedium,
                                    end = RadixTheme.dimensions.paddingMedium,
                                    bottom = RadixTheme.dimensions.paddingMedium
                                )
                                .onSizeChanged { size ->
                                    accountCardHeight = size.height
                                }
                        )
                    }
                    if (state.filters.isAnyFilterSet) {
                        FiltersStrip(
                            historyFilters = state.filters,
                            userInteractionEnabled = state.shouldEnableUserInteraction,
                            onTransactionTypeFilterRemoved = {
                                onTransactionTypeFilterSelected(null)
                                onShowResults()
                            },
                            onTransactionClassFilterRemoved = {
                                onTransactionClassFilterSelected(null)
                                onShowResults()
                            },
                            onResourceFilterRemoved = {
                                onResourceFilterSelected(null)
                                onShowResults()
                            }
                        )
                    }
                    if (state.timeFilterItems.isNotEmpty()) {
                        TimePicker(
                            timeFilterState = timeFilterScrollState,
                            timeFilterItems = state.timeFilterItems,
                            onTimeFilterSelected = onTimeFilterSelected
                        )
                    }
                }
            }
        }
    }

    if (state.shouldShowFiltersSheet) {
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
        modifier = modifier
            .fillMaxWidth()
            .padding(RadixTheme.dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text(
            text = stringResource(id = R.string.transactionHistory_noTransactions),
            style = RadixTheme.typography.secondaryHeader.copy(fontSize = 20.sp),
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
private fun TimePicker(
    modifier: Modifier = Modifier,
    timeFilterState: LazyListState,
    timeFilterItems: ImmutableList<Selectable<State.MonthFilter>>,
    onTimeFilterSelected: (State.MonthFilter) -> Unit
) {
    LazyRow(
        modifier = modifier
            .background(RadixTheme.colors.defaultBackground)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        state = timeFilterState,
        contentPadding = PaddingValues(
            start = RadixTheme.dimensions.paddingMedium,
            end = RadixTheme.dimensions.paddingMedium,
            bottom = RadixTheme.dimensions.paddingMedium
        ),
        content = {
            items(timeFilterItems) { item ->
                Text(
                    modifier = Modifier
                        .clip(RadixTheme.shapes.circle)
                        .clickable {
                            item.data
                            onTimeFilterSelected(item.data)
                        }
                        .applyIf(
                            item.selected,
                            Modifier.background(RadixTheme.colors.gray4, RadixTheme.shapes.circle)
                        )
                        .padding(
                            horizontal = RadixTheme.dimensions.paddingDefault,
                            vertical = RadixTheme.dimensions.paddingSmall
                        ),
                    text = item.data.month,
                    style = RadixTheme.typography.body2HighImportance,
                    maxLines = 1,
                    color = if (item.selected) RadixTheme.colors.gray1 else RadixTheme.colors.gray2,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
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
private fun MonitorListScroll(
    state: LazyListState,
    fixedListElements: Int,
    onScrollEvent: (ScrollInfo) -> Unit,
    onLoadMore: (ScrollInfo.Direction) -> Unit,
    loadThreshold: Int = 6
) {
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
    val loadMoreDown by remember(state) {
        derivedStateOf {
            val lastItem = state.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            val threshold = state.layoutInfo.totalItemsCount - loadThreshold - 1
            if (threshold <= fixedListElements) {
                return@derivedStateOf false
            }
            state.layoutInfo.totalItemsCount > fixedListElements && lastItem.index >= threshold
        }
    }
    val loadMoreUp by remember(state) {
        derivedStateOf {
            val firstItem = state.layoutInfo.visibleItemsInfo.firstOrNull {
                if (fixedListElements == 0) {
                    true
                } else {
                    it.index > fixedListElements - 1
                }
            } ?: return@derivedStateOf false
            state.layoutInfo.totalItemsCount > fixedListElements && firstItem.index <= fixedListElements + loadThreshold
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

    LaunchedEffect(loadMoreDown) {
        if (loadMoreDown) {
            onLoadMore(ScrollInfo.Direction.DOWN)
        }
    }
    LaunchedEffect(loadMoreUp) {
        if (loadMoreUp) {
            onLoadMore(ScrollInfo.Direction.UP)
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
                content = State.Content.Loading,
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
            listState = rememberLazyListState(),
            timeFilterScrollState = rememberLazyListState(),
            onMessageShown = {},
            onRefresh = {}
        )
    }
}
