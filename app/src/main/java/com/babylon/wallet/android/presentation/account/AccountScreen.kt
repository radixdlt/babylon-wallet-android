package com.babylon.wallet.android.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.account.composable.FungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.account.composable.NonFungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.AssetUiModel
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.composables.NftListContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.ScrollableHeaderView
import com.babylon.wallet.android.presentation.ui.composables.ScrollableHeaderViewScrollState
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onAccountPreferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    val state by viewModel.accountUiState.collectAsStateWithLifecycle()
    SetStatusBarColor(color = Color.Transparent, useDarkIcons = !isSystemInDarkTheme())
    AccountScreenContent(
        accountName = state.accountName,
        onAccountPreferenceClick = {
            onAccountPreferenceClick(state.accountAddressFull)
        },
        onBackClick = onBackClick,
        isLoading = state.isLoading,
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        accountAddress = state.accountAddressFull,
        xrdToken = state.xrdToken,
        fungibleTokens = state.fungibleTokens,
        nonFungibleTokens = state.nonFungibleTokens,
        onCopyAccountAddress = viewModel::onCopyAccountAddress,
        gradientIndex = state.gradientIndex,
        onHistoryClick = {},
        onTransferClick = {},
        onFungibleTokenClick = viewModel::onFungibleTokenClick,
        assetDetails = state.assetDetails,
        onNftClick = viewModel::onNonFungibleTokenClick,
        selectedNft = state.selectedNft,
        walletFiatBalance = state.walletFiatBalance,
        modifier = modifier
    )
}

@Composable
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
private fun AccountScreenContent(
    accountName: String,
    onAccountPreferenceClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    accountAddress: String,
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftCollectionUiModel>,
    onCopyAccountAddress: (String) -> Unit,
    gradientIndex: Int,
    onHistoryClick: () -> Unit,
    onTransferClick: () -> Unit,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    assetDetails: AssetUiModel?,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    selectedNft: NftCollectionUiModel.NftItemUiModel?,
    walletFiatBalance: String?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(Brush.horizontalGradient(AccountGradientList[gradientIndex]))
    ) {
        val bottomSheetState =
            rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
        val scope = rememberCoroutineScope()
        val sheetHeight = maxHeight * 0.9f
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetBackgroundColor = RadixTheme.colors.defaultBackground,
            scrimColor = Color.Black.copy(alpha = 0.3f),
            sheetShape = RadixTheme.shapes.roundedRectTopDefault,
            sheetContent = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (assetDetails) {
                        is NftCollectionUiModel -> {
                            selectedNft?.let { selectedNft ->
                                NonFungibleTokenBottomSheetDetails(asset = assetDetails, onCloseClick = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                }, modifier = Modifier.fillMaxSize(), selectedNft = selectedNft)
                            }
                        }
                        is TokenUiModel -> {
                            FungibleTokenBottomSheetDetails(token = assetDetails, onCloseClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }
                            }, onCopyAccountAddress, modifier = Modifier.fillMaxSize())
                        }
                        null -> {}
                    }
                }
            },
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    modifier = Modifier
//                        .systemBarsPadding()
                        .navigationBarsPadding()
                        .fillMaxSize(),
                    topBar = {
                        RadixCenteredTopAppBar(
                            title = accountName,
                            onBackClick = onBackClick,
                            containerColor = Color.Transparent,
                            contentColor = RadixTheme.colors.white,
                            actions = {
                                IconButton(onClick = { onAccountPreferenceClick() }) {
                                    Icon(
                                        painterResource(
                                            id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz
                                        ),
                                        tint = RadixTheme.colors.white,
                                        contentDescription = "account settings"
                                    )
                                }
                            }
                        )
                    },
                    backgroundColor = Color.Transparent
                ) { innerPadding ->
                    // TODO I can't make new swipe to refresh work with development preview banner,
                    //  using accompanist for now
                    val state = rememberSwipeRefreshState(isRefreshing = isRefreshing)
                    SwipeRefresh(
                        modifier = Modifier.fillMaxSize(),
                        state = state,
                        onRefresh = onRefresh,
                        indicatorPadding = innerPadding,
                        indicator = { s, dp ->
                            SwipeRefreshIndicator(
                                state = s,
                                refreshTriggerDistance = dp,
                                contentColor = RadixTheme.colors.gray1,
                                backgroundColor = RadixTheme.colors.defaultBackground,
                            )
                        },
                        refreshTriggerDistance = 100.dp,
                        content = {
                            AccountContent(
                                onCopyAccountAddressClick = onCopyAccountAddress,
                                accountAddress = accountAddress,
                                xrdToken = xrdToken,
                                fungibleTokens = fungibleTokens,
                                nonFungibleTokens = nonFungibleTokens,
                                onTransferClick = onTransferClick,
                                onFungibleTokenClick = {
                                    onFungibleTokenClick(it)
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                onNftClick = { nftCollection, nftItem ->
                                    onNftClick(nftCollection, nftItem)
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                walletFiatBalance = walletFiatBalance,
                                modifier = Modifier.fillMaxSize(),
                                isLoading = isLoading
                            )
//                        PullRefreshIndicator(
//                            refreshing = isRefreshing,
//                            state = pullRefreshState,
//                            contentColor = RadixTheme.colors.gray1,
//                            backgroundColor = RadixTheme.colors.defaultBackground,
//                            modifier = Modifier.align(Alignment.TopCenter)
//                        )
                            if (isLoading) {
                                FullscreenCircularProgressContent()
                            }
                        }
                    )
                }
            }
        }
        AnimatedVisibility(modifier = Modifier.align(Alignment.BottomCenter), visible = false, enter = fadeIn()) {
            HistoryButton(
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingXXLarge)
                    .size(174.dp, 50.dp),
                onHistoryClick
            )
        }
    }
}

@Composable
private fun HistoryButton(modifier: Modifier = Modifier, onHistoryClick: () -> Unit) {
    RadixSecondaryButton(
        text = stringResource(id = R.string.history),
        onClick = onHistoryClick,
        modifier = modifier,
        containerColor = RadixTheme.colors.gray2,
        contentColor = RadixTheme.colors.white,
        shape = RadixTheme.shapes.circle
    ) {
        Icon(
            painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_watch_later),
            tint = RadixTheme.colors.white,
            contentDescription = null
        )
    }
}

@Composable
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
fun AccountContentWithScrollableHeader(
    onRefresh: () -> Unit,
    headerScrollState: ScrollableHeaderViewScrollState,
    accountName: String,
    onBackClick: () -> Unit,
    onAccountPreferenceClick: () -> Unit,
    accountAddress: String,
    walletFiatBalance: String?,
    onCopyAccountAddress: (String) -> Unit,
    onTransferClick: () -> Unit,
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftCollectionUiModel>,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    density: Density,
    isLoading: Boolean,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        ScrollableHeaderView(
            modifier = Modifier.fillMaxWidth(),
            state = headerScrollState,
            header = {
                Column(Modifier.fillMaxWidth()) {
                    RadixCenteredTopAppBar(
                        title = accountName,
                        onBackClick = onBackClick,
                        actions = {
                            IconButton(onClick = { onAccountPreferenceClick() }) {
                                Icon(
                                    painterResource(
                                        id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz
                                    ),
                                    tint = RadixTheme.colors.white,
                                    contentDescription = "account settings"
                                )
                            }
                        }
                    )
                    AccountSummaryContent(
                        modifier = Modifier.fillMaxWidth(),
                        accountAddress = accountAddress,
                        walletFiatBalance = walletFiatBalance,
                        onCopyAccountAddressClick = onCopyAccountAddress,
                        onTransferClick = onTransferClick
                    )
                }
            },
            content = {
                AssetsContent(
                    xrdToken = xrdToken,
                    fungibleTokens = fungibleTokens,
                    nonFungibleTokens = nonFungibleTokens,
                    onFungibleTokenClick = onFungibleTokenClick,
                    modifier = Modifier
                        .background(
                            color = RadixTheme.colors.gray5,
                            shape = RadixTheme.shapes.roundedRectTopDefault
                        )
                        .clip(RadixTheme.shapes.roundedRectTopDefault),
                    onNftClick = onNftClick,
                    isLoading = isLoading
                )
            },
            topBarHeightPx = with(density) { 64.dp.toPx() }.toInt()
        )
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            contentColor = RadixTheme.colors.gray1,
            backgroundColor = RadixTheme.colors.defaultBackground,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        )
    }
}

@ExperimentalPagerApi
@Composable
private fun AccountContent(
    onCopyAccountAddressClick: (String) -> Unit,
    accountAddress: String,
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftCollectionUiModel>,
    onTransferClick: () -> Unit,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    walletFiatBalance: String?,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountSummaryContent(
            modifier = Modifier.fillMaxWidth(),
            accountAddress = accountAddress,
            walletFiatBalance = walletFiatBalance,
            onCopyAccountAddressClick = onCopyAccountAddressClick,
            onTransferClick = onTransferClick
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        AssetsContent(
            xrdToken = xrdToken,
            fungibleTokens = fungibleTokens,
            nonFungibleTokens = nonFungibleTokens,
            onFungibleTokenClick = onFungibleTokenClick,
            modifier = Modifier
                .weight(1f)
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectTopDefault
                )
                .clip(RadixTheme.shapes.roundedRectTopDefault),
            onNftClick = onNftClick,
            isLoading = isLoading
        )
    }
}

@Composable
private fun AccountSummaryContent(
    modifier: Modifier,
    accountAddress: String,
    walletFiatBalance: String?,
    onCopyAccountAddressClick: (String) -> Unit,
    onTransferClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountAddressView(
            address = accountAddress,
            onCopyAccountAddressClick = onCopyAccountAddressClick,
            contentColor = RadixTheme.colors.white
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        walletFiatBalance?.let { value ->
            WalletBalanceView(
                currencySignValue = "$",
                amount = value,
                hidden = false,
                balanceClicked = {},
                contentColor = RadixTheme.colors.white
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            RadixSecondaryButton(
                text = stringResource(id = R.string.account_transfer_button_title),
                onClick = onTransferClick,
                containerColor = RadixTheme.colors.white.copy(alpha = 0.2f),
                contentColor = RadixTheme.colors.white,
                shape = RadixTheme.shapes.circle
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_transfer),
                    tint = RadixTheme.colors.white,
                    contentDescription = null
                )
            }
        }
    }
}

@ExperimentalPagerApi
@Composable
fun AssetsContent(
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftCollectionUiModel>,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    modifier: Modifier = Modifier,
    onNftClick: (NftCollectionUiModel, NftCollectionUiModel.NftItemUiModel) -> Unit,
    isLoading: Boolean,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val pagerState = rememberPagerState()
        val scope = rememberCoroutineScope()
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        TabRow(
            modifier = Modifier.height(50.dp).width(200.dp),
            selectedTabIndex = pagerState.currentPage,
            divider = {}, /* Disable the built-in divider */
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .pagerTabIndicatorOffset(pagerState, tabPositions)
                            .fillMaxHeight()
                            .zIndex(-1f)
                            .background(RadixTheme.colors.gray1, RadixTheme.shapes.circle)
                    )
                }
            },
            backgroundColor = Color.Transparent,
        ) {
            AssetTypeTab.values().forEachIndexed { index, assetTypeTab ->
                val selected = index == pagerState.currentPage
                Tab(
                    selected = selected,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    interactionSource = MutableInteractionSource()
                ) {
                    Text(
                        modifier = Modifier.padding(RadixTheme.dimensions.paddingMedium),
                        text = stringResource(id = assetTypeTab.stringId),
                        style = RadixTheme.typography.body1HighImportance,
                        color = if (selected) RadixTheme.colors.white else RadixTheme.colors.gray1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            count = AssetTypeTab.values().size,
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
                content = {
                    when (AssetTypeTab.values()[page]) {
                        AssetTypeTab.TOKEN_TAB -> {
                            TokenListContent(
                                tokenItems = fungibleTokens,
                                xrdTokenUi = xrdToken,
                                modifier = Modifier.fillMaxSize(),
                                onFungibleTokenClick = onFungibleTokenClick
                            )
                        }
                        AssetTypeTab.NTF_TAB -> {
                            val collapsedState =
                                remember(nonFungibleTokens) { nonFungibleTokens.map { true }.toMutableStateList() }
                            NftListContent(
                                collapsedState = collapsedState,
                                item = nonFungibleTokens,
                                modifier = Modifier.fillMaxSize(),
                                onNftClick = onNftClick
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun AccountContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountScreenContent(
                accountName = randomTokenAddress(),
                onAccountPreferenceClick = {},
                onBackClick = {},
                isLoading = false,
                isRefreshing = false,
                onRefresh = {},
                accountAddress = randomTokenAddress(),
                xrdToken = sampleFungibleTokens().first().toTokenUiModel(),
                fungibleTokens = sampleFungibleTokens().map { it.toTokenUiModel() }.toPersistentList(),
                nonFungibleTokens = persistentListOf(),
                onCopyAccountAddress = {},
                gradientIndex = 0,
                onHistoryClick = {},
                onTransferClick = {},
                onFungibleTokenClick = {},
                assetDetails = null,
                onNftClick = { _, _ -> },
                selectedNft = null,
                walletFiatBalance = "1000",
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountContentDarkPreview() {
    RadixWalletTheme(darkTheme = true) {
        with(SampleDataProvider()) {
            AccountScreenContent(
                accountName = randomTokenAddress(),
                onAccountPreferenceClick = {},
                onBackClick = {},
                isLoading = false,
                isRefreshing = false,
                onRefresh = {},
                accountAddress = randomTokenAddress(),
                xrdToken = sampleFungibleTokens().first().toTokenUiModel(),
                fungibleTokens = sampleFungibleTokens().map { it.toTokenUiModel() }.toPersistentList(),
                nonFungibleTokens = persistentListOf(),
                onCopyAccountAddress = {},
                gradientIndex = 0,
                onHistoryClick = {},
                onTransferClick = {},
                onFungibleTokenClick = {},
                assetDetails = null,
                onNftClick = { _, _ -> },
                selectedNft = null,
                walletFiatBalance = "1000",
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FungibleTokenDetailsDarkPreview() {
    RadixWalletTheme(darkTheme = true) {
        with(SampleDataProvider()) {
            FungibleTokenBottomSheetDetails(
                modifier = Modifier.fillMaxSize(),
                token = sampleFungibleTokens().first().toTokenUiModel(),
                onCloseClick = {},
                onCopyAccountAddress = {}
            )
        }
    }
}
