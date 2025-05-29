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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixTextButton
import com.babylon.wallet.android.designsystem.composable.RadixTextField
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.domain.model.DirectoryDefinition
import com.babylon.wallet.android.presentation.account.composable.HistoryFilterTag
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.DefaultModalSheetLayout
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.Sample
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.coroutines.launch
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import com.babylon.wallet.android.designsystem.R as DSR

@Composable
fun DAppDirectoryScreen(
    viewModel: DAppDirectoryViewModel,
    onDAppClick: (address: AccountAddress) -> Unit
) {
    val state: DAppDirectoryViewModel.State by viewModel.state.collectAsStateWithLifecycle()

    DAppDirectoryContent(
        state = state,
        onDAppClick = onDAppClick,
        onRefresh = viewModel::onRefresh,
        onSearchTermUpdated = viewModel::onSearchTermUpdated,
        onFilterTagAdded = viewModel::onFilterTagAdded,
        onFilterTagRemoved = viewModel::onFilterTagRemoved,
        onAllFilterTagsRemoved = viewModel::onAllFilterTagsRemoved,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
private fun DAppDirectoryContent(
    modifier: Modifier = Modifier,
    state: DAppDirectoryViewModel.State,
    onRefresh: () -> Unit,
    onDAppClick: (AccountAddress) -> Unit,
    onSearchTermUpdated: (String) -> Unit,
    onFilterTagAdded: (String) -> Unit,
    onFilterTagRemoved: (String) -> Unit,
    onAllFilterTagsRemoved: () -> Unit,
    onMessageShown: () -> Unit
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

    val focusManager = LocalFocusManager.current
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
                    windowInsets = WindowInsets.statusBarsAndBanner,
                    actions = {
                        AnimatedVisibility(
                            visible = !state.isLoadingDirectory,
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
                )

                RadixTextField(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth(),
                    value = state.filters.searchTerm,
                    onValueChanged = onSearchTermUpdated,
                    enabled = !state.isLoadingDirectory,
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
                                tint = if (state.isLoadingDirectory) {
                                    RadixTheme.colors.backgroundTertiary
                                } else {
                                    RadixTheme.colors.icon
                                }
                            )
                        }
                    }
                )

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
                if (state.isLoadingDirectory) {
                    List(10) {
                        null
                    }
                } else {
                    state.directory
                }
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        focusManager.clearFocus()
                        return super.onPreScroll(available, source)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .pullToRefresh(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh
                    ),
                contentPadding = padding.plus(
                    PaddingValues(RadixTheme.dimensions.paddingDefault)
                ),
                userScrollEnabled = !state.isLoadingDirectory,
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
            ) {
                items(
                    items = directory,
                ) { details ->
                    DAppCard(
                        modifier = Modifier.fillMaxWidth(),
                        details = details,
                        onClick = {
                            if (details != null) {
                                onDAppClick(details.directoryDefinition.dAppDefinitionAddress)
                            }
                        }
                    )
                }
            }

            if (state.errorLoadingDirectory) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall)
                ) {
                    Icon(
                        modifier = Modifier.size(44.dp),
                        painter = painterResource(DSR.drawable.ic_factory_reset),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )

                    Text(
                        text = stringResource(R.string.dappDirectory_error_heading),
                        color = RadixTheme.colors.text
                    )

                    Text(
                        text = stringResource(R.string.dappDirectory_error_message),
                        color = RadixTheme.colors.textSecondary
                    )
                }
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
            userScrollEnabled = !state.isLoadingDirectory
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
    details: DirectoryDAppWithDetails?,
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
                    .radixPlaceholder(visible = details?.dApp == null),
                dAppIconUrl = details?.dApp?.iconUrl,
                dAppName = details?.directoryDefinition?.name.orEmpty(),
                shape = RadixTheme.shapes.roundedRectSmall
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction = if (details?.dApp == null) 0.5f else 1f
                        )
                        .radixPlaceholder(visible = details?.dApp == null),
                    text = details?.dApp?.name.orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.text,
                    maxLines = if (details?.dApp == null) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                val description = details?.dApp?.description
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

@Preview
@Composable
fun DAppDirectoryPreviewLight() {
    RadixWalletPreviewTheme {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = false,
                directory = listOf(
                    DirectoryDAppWithDetails.sample(),
                    DirectoryDAppWithDetails.sample.other()
                ),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
fun DAppDirectoryPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = false,
                directory = listOf(
                    DirectoryDAppWithDetails.sample(),
                    DirectoryDAppWithDetails.sample.other()
                ),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
fun DAppDirectoryWithFiltersPreviewLight() {
    RadixWalletPreviewTheme {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = false,
                directory = listOf(
                    DirectoryDAppWithDetails.sample(),
                ),
                filters = DAppDirectoryFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
fun DAppDirectoryWithFiltersPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = false,
                directory = listOf(
                    DirectoryDAppWithDetails.sample(),
                ),
                filters = DAppDirectoryFilters(
                    searchTerm = "awe",
                    selectedTags = setOf("DeFi", "Token")
                ),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
fun DAppDirectoryErrorPreviewLight() {
    RadixWalletPreviewTheme {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = true,
                directory = listOf(),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@Preview
@Composable
fun DAppDirectoryErrorPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        DAppDirectoryContent(
            state = DAppDirectoryViewModel.State(
                isLoadingDirectory = false,
                isRefreshing = false,
                errorLoadingDirectory = true,
                directory = listOf(),
                filters = DAppDirectoryFilters(),
                uiMessage = null
            ),
            onDAppClick = {},
            onRefresh = {},
            onSearchTermUpdated = {},
            onFilterTagAdded = {},
            onFilterTagRemoved = {},
            onAllFilterTagsRemoved = {},
            onMessageShown = {}
        )
    }
}

@UsesSampleValues
val DirectoryDAppWithDetails.Companion.sample: Sample<DirectoryDAppWithDetails>
    get() = object : Sample<DirectoryDAppWithDetails> {
        override fun invoke(): DirectoryDAppWithDetails = DirectoryDAppWithDetails(
            directoryDefinition = DirectoryDefinition(
                name = "Awesome DApp",
                dAppDefinitionAddress = AccountAddress.sampleMainnet(),
                tags = listOf("DeFi", "token")
            ),
            isHighlighted = true,
            details = DirectoryDAppWithDetails.Details.Data(
                dApp = DApp(
                    dAppAddress = AccountAddress.sampleMainnet(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.NAME.key,
                            value = "Awesome DApp",
                            valueType = MetadataType.String
                        ),
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.DESCRIPTION.key,
                            value = "Awesome DApp is an awesome dApp for trading on Radix.",
                            valueType = MetadataType.String
                        )
                    )
                )
            )
        )

        override fun other(): DirectoryDAppWithDetails = DirectoryDAppWithDetails(
            directoryDefinition = DirectoryDefinition(
                name = "Dashboard",
                dAppDefinitionAddress = AccountAddress.sampleMainnet.other(),
                tags = listOf("explorer")
            ),
            isHighlighted = false,
            details = DirectoryDAppWithDetails.Details.Data(
                dApp = DApp(
                    dAppAddress = AccountAddress.sampleMainnet.other(),
                    metadata = listOf(
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.NAME.key,
                            value = "Dashboard",
                            valueType = MetadataType.String
                        ),
                        Metadata.Primitive(
                            key = ExplicitMetadataKey.DESCRIPTION.key,
                            value = "Explore assets and transactions on Radix",
                            valueType = MetadataType.String
                        )
                    )
                )
            )
        )
    }
