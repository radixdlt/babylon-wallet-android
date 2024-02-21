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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.BalanceChange
import com.babylon.wallet.android.domain.model.HistoryFilters
import com.babylon.wallet.android.domain.model.Selectable
import com.babylon.wallet.android.domain.model.TransactionClass
import com.babylon.wallet.android.domain.model.TransactionHistoryItem
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.NonFungibleCollection
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.Token
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.presentation.history.composables.FiltersDialog
import com.babylon.wallet.android.presentation.history.composables.FiltersStrip
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.SimpleAccountCard
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.assets.name
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.utils.ledgerLastUsedDateFormat
import com.babylon.wallet.android.utils.openUrl
import com.babylon.wallet.android.utils.timestampHoursMinutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import rdx.works.core.displayableQuantity

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        onSubmittedByFilterChanged = viewModel::onSubmittedByFilterChanged
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect { event ->
            when (event) {
                is HistoryEvent.OnTransactionItemClick -> context.openUrl(event.url)
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
    onSubmittedByFilterChanged: (HistoryFilters.SubmittedBy?) -> Unit
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
    val listState = rememberLazyListState()
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
                                    text = item.item.toInstant().ledgerLastUsedDateFormat(),
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
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingLarge)
    ) {
        Image(
            modifier = Modifier.height(130.dp),
            painter = painterResource(id = R.drawable.ic_empty_fungibles),
            contentDescription = null
        )

        Text(
            text = "There are not transactions for this account",
            style = RadixTheme.typography.header,
            color = RadixTheme.colors.gray1
        )
    }
}

@Composable
private fun HistoryTransactionItem(modifier: Modifier = Modifier, transactionItem: TransactionHistoryItem, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RadixTheme.shapes.roundedRectMedium)
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.defaultBackground,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        val withdrawn = remember(transactionItem.withdrawn) {
            transactionItem.withdrawn
        }
        val deposited = remember(transactionItem.deposited) {
            transactionItem.deposited
        }
        if (transactionItem.noBalanceChanges) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TypeAndTimestampLabel(item = transactionItem)
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = RadixTheme.dimensions.paddingMedium)
                    .fillMaxWidth()
                    .border(1.dp, RadixTheme.colors.gray4, shape = RadixTheme.shapes.roundedRectMedium)
                    .padding(RadixTheme.dimensions.paddingMedium),
                text = "No deposits or withdrawals from this account in this transaction.", // TODO crowding
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1
            )
        } else {
            val withdrawnShown = withdrawn.isNotEmpty()
            if (withdrawnShown) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    Icon(painter = painterResource(id = DSR.ic_tx_withdrawn), contentDescription = null, tint = Color.Unspecified)
                    Text(
                        text = "Withdrawn", // TODO crowdin
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.gray1
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TypeAndTimestampLabel(item = transactionItem)
                }
                Column(
                    modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                ) {
                    val lastItem = withdrawn.last()
                    withdrawn.forEach { withdraw ->
                        val addDivider = lastItem != withdraw
                        BalanceChangeItem(withdraw)
                        if (addDivider) {
                            HorizontalDivider(color = RadixTheme.colors.gray3)
                        }
                    }
                }
            }
            if (deposited.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    Icon(painter = painterResource(id = DSR.ic_tx_deposited), contentDescription = null, tint = Color.Unspecified)
                    Text(
                        text = "Deposited", // TODO crowdin
                        style = RadixTheme.typography.body2Header,
                        color = RadixTheme.colors.green1
                    )
                    if (withdrawnShown.not()) {
                        Spacer(modifier = Modifier.weight(1f))
                        TypeAndTimestampLabel(item = transactionItem)
                    }
                }
                Column(
                    modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                ) {
                    val lastItem = deposited.last()
                    deposited.forEach { deposited ->
                        val addDivider = lastItem != deposited
                        BalanceChangeItem(deposited)
                        if (addDivider) {
                            HorizontalDivider(color = RadixTheme.colors.gray3)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceChangeItem(balanceChange: BalanceChange) {
    when (balanceChange) {
        is BalanceChange.FungibleBalanceChange -> {
            when (val asset = balanceChange.asset) {
                is LiquidStakeUnit -> {
                    LiquidStakeUnitBalanceChange(asset)
                }

                is PoolUnit -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(RadixTheme.dimensions.paddingMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                        ) {
                            Thumbnail.PoolUnit(
                                modifier = Modifier.size(42.dp),
                                poolUnit = asset
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = asset.name(),
                                    style = RadixTheme.typography.body1Header,
                                    color = RadixTheme.colors.gray1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                val associatedDAppName = remember(asset) {
                                    asset.pool?.associatedDApp?.name
                                }
                                if (!associatedDAppName.isNullOrEmpty()) {
                                    Text(
                                        text = associatedDAppName,
                                        style = RadixTheme.typography.body2Regular,
                                        color = RadixTheme.colors.gray2,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier,
                                text = balanceChange.balanceChange.abs().displayableQuantity(),
                                style = RadixTheme.typography.body1Header,
                                color = RadixTheme.colors.gray1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = R.string.transactionReview_worth).uppercase(),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray2,
                            maxLines = 1
                        )
                        val poolResources = asset.pool?.resources.orEmpty()
                        Column(modifier = Modifier.border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)) {
                            poolResources.forEachIndexed { index, item ->
                                val addDivider = index != poolResources.lastIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = RadixTheme.dimensions.paddingDefault,
                                            vertical = RadixTheme.dimensions.paddingMedium
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                                ) {
                                    Thumbnail.Fungible(
                                        modifier = Modifier.size(24.dp),
                                        token = item,
                                    )
                                    Text(
                                        text = item.displayTitle,
                                        style = RadixTheme.typography.body2HighImportance,
                                        color = RadixTheme.colors.gray1,
                                        maxLines = 2
                                    )
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = asset.resourceRedemptionValue(item)?.displayableQuantity().orEmpty(),
                                        style = RadixTheme.typography.body1HighImportance,
                                        color = RadixTheme.colors.gray1,
                                        textAlign = TextAlign.End,
                                        maxLines = 2
                                    )
                                }
                                if (addDivider) {
                                    HorizontalDivider(color = RadixTheme.colors.gray3)
                                }
                            }
                        }
                    }
                }

                is Token -> {
                    TokenContent(
                        resource = asset.resource,
                        withdraw = balanceChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingMedium)
                    )
                }

                else -> {}
            }
        }

        is BalanceChange.NonFungibleBalanceChange -> {
            when (val asset = balanceChange.asset) {
                is NonFungibleCollection -> {
                    asset.resource.items.forEachIndexed { _, nftItem ->
                        val addDivider = nftItem != asset.resource.items.last()
                        NftItemBalanceChange(nftItem, asset)
                        if (addDivider) {
                            HorizontalDivider(color = RadixTheme.colors.gray3)
                        }
                    }
                }

                is StakeClaim -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(RadixTheme.dimensions.paddingMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                        ) {
                            Thumbnail.NonFungible(
                                modifier = Modifier.size(42.dp),
                                collection = asset.resource
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = asset.resource.name.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
                                    style = RadixTheme.typography.body1Header,
                                    color = RadixTheme.colors.gray1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = asset.validator.name,
                                    style = RadixTheme.typography.body2Regular,
                                    color = RadixTheme.colors.gray2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = RadixTheme.dimensions.paddingSmall),
                            text = stringResource(id = R.string.transactionReview_toBeClaimed).uppercase(),
                            style = RadixTheme.typography.body2HighImportance,
                            color = RadixTheme.colors.gray2,
                            maxLines = 1
                        )
                        asset.resource.items.forEachIndexed { index, item ->
                            val addSpacer = index != asset.resource.items.lastIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                                    .padding(RadixTheme.dimensions.paddingMedium),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
                            ) {
                                Icon(
                                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RadixTheme.shapes.circle),
                                    tint = Color.Unspecified
                                )
                                Text(
                                    text = XrdResource.SYMBOL,
                                    style = RadixTheme.typography.body2HighImportance,
                                    color = RadixTheme.colors.gray1,
                                    maxLines = 2
                                )
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.claimAmountXrd?.displayableQuantity().orEmpty(),
                                    style = RadixTheme.typography.body1HighImportance,
                                    color = RadixTheme.colors.gray1,
                                    textAlign = TextAlign.End,
                                    maxLines = 2
                                )
                            }
                            if (addSpacer) {
                                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                            }
                        }
                    }
                }

                null -> TODO()
            }
        }
    }
}

@Composable
private fun LiquidStakeUnitBalanceChange(
    asset: LiquidStakeUnit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.LSU(
                modifier = Modifier.size(42.dp),
                liquidStakeUnit = asset,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = asset.fungibleResource.displayTitle.ifEmpty {
                        stringResource(
                            id = R.string.transactionReview_unknown
                        )
                    },
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = asset.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.transactionReview_worth).uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                .padding(RadixTheme.dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Icon(
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RadixTheme.shapes.circle),
                tint = Color.Unspecified
            )
            Text(
                text = XrdResource.SYMBOL,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
            Text(
                modifier = Modifier.weight(1f),
                text = asset.stakeValue()?.displayableQuantity().orEmpty(),
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1,
                textAlign = TextAlign.End,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NftItemBalanceChange(
    nftItem: Resource.NonFungibleResource.Item,
    asset: NonFungibleCollection,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(RadixTheme.dimensions.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.NonFungible(
            modifier = Modifier.size(24.dp),
            collection = asset.collection
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = nftItem.localId.displayable,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = (nftItem.name ?: asset.resource.name).ifEmpty {
                    stringResource(id = R.string.transactionReview_unknown)
                },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TokenContent(
    resource: Resource.FungibleResource,
    withdraw: BalanceChange.FungibleBalanceChange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumbnail.Fungible(token = resource, modifier = Modifier.size(44.dp))
        Text(
            text = resource.displayTitle,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = withdraw.balanceChange.abs().displayableQuantity(),
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TransactionClass.name(): String {
    return when (this) {
        TransactionClass.General -> "General"
        TransactionClass.PoolContribution -> "Contribute"
        TransactionClass.PoolRedemption -> "Redeem"
        TransactionClass.Transfer -> "Transfers"
        TransactionClass.ValidatorClaim -> "Claim"
        TransactionClass.ValidatorStake -> "Stake"
        TransactionClass.ValidatorUnstake -> "Unstake"
        TransactionClass.AccountDespositSettingsUpdate -> "Third-party Deposit Settings"
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

@Composable
private fun TypeAndTimestampLabel(modifier: Modifier = Modifier, item: TransactionHistoryItem) {
    val text = buildAnnotatedString {
        withStyle(style = RadixTheme.typography.body2HighImportance.toSpanStyle()) {
            append(item.transactionClass.description())
        }
        item.timestamp?.timestampHoursMinutes()?.let {
            append(" ")
            append(it)
        }
    }
    Text(
        modifier = modifier,
        text = text,
        style = RadixTheme.typography.body2Regular,
        maxLines = 1,
        color = RadixTheme.colors.gray2
    )
}

@Composable
private fun TransactionClass?.description(): String {
    return when (this) {
        TransactionClass.General -> stringResource(id = R.string.history_transactionClassGeneral)
        TransactionClass.Transfer -> stringResource(id = R.string.history_transactionClassTransfer)
        TransactionClass.PoolContribution -> stringResource(id = R.string.history_transactionClassContribute)
        TransactionClass.PoolRedemption -> stringResource(id = R.string.history_transactionClassRedeem)
        TransactionClass.ValidatorStake -> stringResource(id = R.string.history_transactionClassStaking)
        TransactionClass.ValidatorUnstake -> stringResource(id = R.string.history_transactionClassUnstaking)
        TransactionClass.ValidatorClaim -> stringResource(id = R.string.history_transactionClassClaim)
        TransactionClass.AccountDespositSettingsUpdate -> stringResource(id = R.string.history_transactionClassAccountSettings)
        else -> stringResource(id = R.string.history_transactionClassOther)
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
            onSubmittedByFilterChanged = {}
        )
    }
}
