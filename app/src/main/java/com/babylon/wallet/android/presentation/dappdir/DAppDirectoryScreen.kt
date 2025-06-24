@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.babylon.wallet.android.presentation.dappdir

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.presentation.account.composable.HistoryFilterTag
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryViewModel.Category
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryViewModel.DAppCategoryType
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryViewModel.DAppDirectoryFilters
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryViewModel.DAppDirectoryTab
import com.babylon.wallet.android.presentation.dappdir.DAppDirectoryViewModel.DAppWithDetails
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.discover.common.views.LoadingErrorView
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.InfoButton
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.utils.clearFocusNestedScrollConnection
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.launch
import com.babylon.wallet.android.designsystem.R as DSR

@Composable
fun DAppDirectoryScreen(
    viewModel: DAppDirectoryViewModel,
    onDAppClick: (address: AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state: DAppDirectoryViewModel.State by viewModel.state.collectAsStateWithLifecycle()

    DAppDirectoryContent(
        state = state,
        onDAppClick = onDAppClick,
        onRefresh = viewModel::onRefresh,
        onTabSelected = viewModel::onTabSelected,
        onSearchTermUpdated = viewModel::onSearchTermUpdated,
        onFilterTagAdded = viewModel::onFilterTagAdded,
        onFilterTagRemoved = viewModel::onFilterTagRemoved,
        onAllFilterTagsRemoved = viewModel::onAllFilterTagsRemoved,
        onMessageShown = viewModel::onMessageShown,
        onInfoClick = onInfoClick
    )
}

@Composable
private fun DAppDirectoryContent(
    modifier: Modifier = Modifier,
    state: DAppDirectoryViewModel.State,
    onRefresh: () -> Unit,
    onTabSelected: (DAppDirectoryTab) -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onSearchTermUpdated: (String) -> Unit,
    onFilterTagAdded: (String) -> Unit,
    onFilterTagRemoved: (String) -> Unit,
    onAllFilterTagsRemoved: () -> Unit,
    onMessageShown: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isFiltersVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isFiltersVisible) {
        if (isFiltersVisible) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    BackHandler(
        enabled = isFiltersVisible,
        onBack = {
            isFiltersVisible = false
        }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = RadixTheme.colors.background)
            ) {
                RadixCenteredTopAppBar(
                    title = stringResource(R.string.dappDirectory_title),
                    onBackClick = {},
                    backIconType = BackIconType.None,
                    windowInsets = WindowInsets.statusBarsAndBanner
                )

                val tabIndex = remember(state.selectedTab) {
                    DAppDirectoryTab.entries.indexOf(state.selectedTab)
                }

                TabRow(
                    modifier = Modifier
                        .padding(horizontal = RadixTheme.dimensions.paddingDefault)
                        .clip(shape = RadixTheme.shapes.roundedRectSmall),
                    selectedTabIndex = tabIndex,
                    containerColor = RadixTheme.colors.unselectedSegmentedControl,
                    divider = {},
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[tabIndex])
                                .fillMaxHeight()
                                .zIndex(-1f)
                                .padding(2.dp)
                                .background(
                                    color = RadixTheme.colors.selectedSegmentedControl,
                                    shape = RadixTheme.shapes.roundedRectSmall
                                )
                        )
                    }
                ) {
                    DAppDirectoryTab.entries.forEach { tab ->
                        val isSelected = tab == state.selectedTab
                        Tab(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    onTabSelected(tab)
                                }
                            },
                            selectedContentColor = RadixTheme.colors.text,
                            unselectedContentColor = RadixTheme.colors.textSecondary
                        ) {
                            Text(
                                modifier = Modifier.padding(
                                    horizontal = RadixTheme.dimensions.paddingMedium,
                                    vertical = RadixTheme.dimensions.paddingSmall
                                ),
                                text = tab.title(),
                                style = if (isSelected) {
                                    RadixTheme.typography.body1Header
                                } else {
                                    RadixTheme.typography.body1Regular
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(vertical = RadixTheme.dimensions.paddingDefault)
                        .padding(
                            start = RadixTheme.dimensions.paddingDefault,
                            end = if (state.isFiltersButtonVisible) {
                                RadixTheme.dimensions.paddingSmall
                            } else {
                                RadixTheme.dimensions.paddingDefault
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadixTextField(
                        modifier = Modifier.weight(1f),
                        value = state.filters.searchTerm,
                        onValueChanged = onSearchTermUpdated,
                        enabled = !state.isLoading,
                        hint = stringResource(R.string.dappDirectory_search_placeholder),
                        trailingIcon = {
                            if (state.filters.searchTerm.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onSearchTermUpdated("")
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(DSR.drawable.ic_close),
                                        contentDescription = null,
                                        tint = RadixTheme.colors.icon
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(DSR.drawable.ic_search),
                                    contentDescription = null,
                                    tint = if (state.isLoading) {
                                        RadixTheme.colors.backgroundTertiary
                                    } else {
                                        RadixTheme.colors.icon
                                    }
                                )
                            }
                        }
                    )

                    if (state.isFiltersButtonVisible) {
                        AnimatedVisibility(
                            visible = !state.isLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = {
                                    isFiltersVisible = true
                                }
                            ) {
                                Icon(
                                    painterResource(
                                        id = DSR.drawable.ic_filter_list
                                    ),
                                    tint = RadixTheme.colors.icon,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                FilterTags(
                    state = state,
                    onFilterTagRemoved = onFilterTagRemoved
                )

                HorizontalDivider(color = RadixTheme.colors.divider)
            }
        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault)
            )
        },
        containerColor = RadixTheme.colors.backgroundSecondary
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        Box {
            val directory = remember(state) {
                if (state.isLoading) {
                    List(10) {
                        null
                    }
                } else {
                    state.items
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(clearFocusNestedScrollConnection())
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh
                    ),
                contentPadding = padding.plus(
                    PaddingValues(RadixTheme.dimensions.paddingDefault)
                ),
                userScrollEnabled = !state.isLoading,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                items(
                    items = directory,
                ) { item ->
                    when (item) {
                        is Category -> Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = item.type.title(),
                            style = RadixTheme.typography.header,
                            color = RadixTheme.colors.text,
                            maxLines = 1
                        )

                        is DAppWithDetails, null -> DAppCard(
                            modifier = Modifier.fillMaxWidth(),
                            details = item,
                            onClick = {
                                if (item != null) {
                                    onDAppClick(item.dAppDefinitionAddress)
                                }
                            }
                        )
                    }
                }
            }

            when {
                state.errorLoading -> LoadingErrorView(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .align(Alignment.Center),
                    title = stringResource(R.string.dappDirectory_error_heading),
                    subtitle = stringResource(R.string.dappDirectory_error_message)
                )

                state.isEmpty && state.selectedTab == DAppDirectoryTab.Approved -> EmptyStateView(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .align(Alignment.Center),
                    title = stringResource(R.string.authorizedDapps_subtitle),
                    onInfoClick = onInfoClick
                )

                state.isEmpty && state.selectedTab == DAppDirectoryTab.All -> EmptyStateView(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .align(Alignment.Center),
                    title = "There are no dApps available", // TODO sergiu localize
                    onInfoClick = onInfoClick
                )
            }

            PullToRefreshDefaults.Indicator(
                modifier = Modifier
                    .padding(padding)
                    .align(Alignment.TopCenter),
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                color = RadixTheme.colors.icon,
                containerColor = RadixTheme.colors.backgroundTertiary
            )
        }
    }

    if (isFiltersVisible) {
        DefaultModalSheetLayout(
            sheetState = sheetState,
            wrapContent = true,
            onDismissRequest = {
                isFiltersVisible = false
            },
            enableImePadding = true,
            sheetContent = {
                Column {
                    RadixCenteredTopAppBar(
                        title = stringResource(R.string.dappDirectory_filters_title),
                        onBackClick = {
                            isFiltersVisible = false
                        },
                        backIconType = BackIconType.Close,
                        actions = {
                            RadixTextButton(
                                text = stringResource(R.string.transactionHistory_filters_clearAll),
                                onClick = onAllFilterTagsRemoved
                            )
                        }
                    )

                    FlowRow(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(RadixTheme.dimensions.paddingDefault),
                        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall),
                        content = {
                            state.filters.availableTags.forEach { tag ->
                                val isSelected = remember(state.filters) {
                                    state.filters.isTagSelected(tag)
                                }

                                HistoryFilterTag(
                                    selected = isSelected,
                                    text = tag,
                                    showCloseIcon = false,
                                    onClick = {
                                        if (isSelected) {
                                            onFilterTagRemoved(tag)
                                        } else {
                                            onFilterTagAdded(tag)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun FilterTags(
    state: DAppDirectoryViewModel.State,
    modifier: Modifier = Modifier,
    onFilterTagRemoved: (String) -> Unit
) {
    val tags = remember(state.filters.selectedTags) {
        state.filters.selectedTags.toList()
    }
    if (tags.isNotEmpty()) {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                RadixTheme.dimensions.paddingMedium
            ),
            contentPadding = PaddingValues(
                start = RadixTheme.dimensions.paddingMedium,
                end = RadixTheme.dimensions.paddingMedium,
                bottom = RadixTheme.dimensions.paddingMedium
            ),
            userScrollEnabled = !state.isLoading
        ) {
            items(items = tags) { tag ->
                HistoryFilterTag(
                    selected = true,
                    showCloseIcon = true,
                    text = tag,
                    onClick = {
                        onFilterTagRemoved(tag)
                    },
                    onCloseClick = {
                        onFilterTagRemoved(tag)
                    }
                )
            }
        }
    }
}

@Composable
private fun DAppCard(
    modifier: Modifier = Modifier,
    details: DAppWithDetails?,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .defaultCardShadow()
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.card,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .throttleClickable(onClick = onClick)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.DApp(
                modifier = Modifier
                    .size(44.dp)
                    .radixPlaceholder(visible = details?.data == null),
                dAppIconUrl = details?.data?.iconUri,
                dAppName = details?.data?.name.orEmpty(),
                shape = RadixTheme.shapes.roundedRectSmall
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction = if (details?.data == null) 0.5f else 1f
                        )
                        .radixPlaceholder(visible = details?.data == null),
                    text = details?.data?.name.orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.text,
                    maxLines = if (details?.data == null) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                val description = details?.data?.description
                if (details == null || details.isFetchingDAppDetails || !description.isNullOrBlank()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .radixPlaceholder(
                                visible = details == null || details.isFetchingDAppDetails
                            ),
                        text = description.orEmpty(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = RadixTheme.colors.text
                    )
                }
            }

            Icon(
                painter = painterResource(
                    id = DSR.drawable.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.icon.copy(alpha = if (details != null) 1f else 0f),
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    onInfoClick: (GlossaryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Text(
            text = title,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        InfoButton(
            text = stringResource(id = R.string.infoLink_title_dapps),
            onClick = { onInfoClick(GlossaryItem.dapps) }
        )
    }
}

@Composable
private fun DAppCategoryType.title() = when (this) { // TODO sergiu localize
    DAppCategoryType.DeFi -> "DeFi"
    DAppCategoryType.Utility -> "Utility"
    DAppCategoryType.Dao -> "Dao"
    DAppCategoryType.NFT -> "NFT"
    DAppCategoryType.Meme -> "Meme"
    DAppCategoryType.Unknown -> "Other"
}

@Composable
private fun DAppDirectoryTab.title() = when (this) {
    DAppDirectoryTab.All -> stringResource(R.string.discover_view_all_dapps)
    DAppDirectoryTab.Approved -> stringResource(R.string.discover_view_approved_dapps)
}

@Preview
@Composable
private fun DAppDirectoryPreviewLight(
    @PreviewParameter(DAppDirectoryPreviewProvider::class) state: DAppDirectoryViewModel.State
) {
    RadixWalletPreviewTheme {
        DAppDirectoryContent(
            state = state,
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {},
            onTabSelected = {},
            onInfoClick = {}
        )
    }
}

@Preview
@Composable
private fun DAppDirectoryPreviewDark(
    @PreviewParameter(DAppDirectoryPreviewProvider::class) state: DAppDirectoryViewModel.State
) {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DAppDirectoryContent(
            state = state,
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {},
            onTabSelected = {},
            onInfoClick = {}
        )
    }
}

class DAppDirectoryPreviewProvider : PreviewParameterProvider<DAppDirectoryViewModel.State> {

    override val values: Sequence<DAppDirectoryViewModel.State>
        get() = sequenceOf(
            DAppDirectoryViewModel.State(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                    Category(
                        type = DAppCategoryType.Unknown
                    ),
                    DAppWithDetails.sample.other()
                ),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            DAppDirectoryViewModel.State(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                ),
                filters = DAppDirectoryFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            DAppDirectoryViewModel.State(
                isLoading = false,
                isRefreshing = false,
                items = listOf(
                    DAppWithDetails.sample(),
                ),
                filters = DAppDirectoryFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            DAppDirectoryViewModel.State(
                isLoading = false,
                isRefreshing = false,
                errorLoadingDirectory = true,
                items = emptyList(),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            DAppDirectoryViewModel.State(
                isLoading = false,
                isRefreshing = false,
                errorLoadingDirectory = false,
                items = emptyList(),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            DAppDirectoryViewModel.State(
                isLoading = true,
                isRefreshing = false,
                errorLoadingDirectory = false,
                items = emptyList(),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            )
        )
}

@UsesSampleValues
val DAppWithDetails.Companion.sample: Sample<DAppWithDetails>
    get() = object : Sample<DAppWithDetails> {

        override fun invoke(): DAppWithDetails = DAppWithDetails(
            dAppDefinitionAddress = AccountAddress.sampleMainnet(),
            details = DAppWithDetails.Details.Data(
                name = "Awesome DApp",
                description = "Awesome DApp is an awesome dApp for trading on Radix.",
                iconUri = null
            )
        )

        override fun other(): DAppWithDetails = DAppWithDetails(
            dAppDefinitionAddress = AccountAddress.sampleMainnet.other(),
            details = DAppWithDetails.Details.Data(
                name = "Dashboard",
                description = "Explore assets and transactions on Radix",
                iconUri = null
            )
        )
    }
