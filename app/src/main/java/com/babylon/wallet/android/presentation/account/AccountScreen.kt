package com.babylon.wallet.android.presentation.account

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.model.AssetUiModel
import com.babylon.wallet.android.presentation.model.NftCollectionUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NftListContent
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.ScrollableHeaderView
import com.babylon.wallet.android.presentation.ui.composables.ScrollableHeaderViewScrollState
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    accountName: String,
    onAccountPreferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state by viewModel.accountUiState.collectAsStateWithLifecycle()
    SetStatusBarColor(color = Color.Transparent, useDarkIcons = !isSystemInDarkTheme())
    AccountScreenContent(
        accountName = accountName,
        onAccountPreferenceClick = {
            onAccountPreferenceClick(state.accountAddressFull)
        },
        onBackClick = onBackClick,
        isLoading = state.isLoading,
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        accountAddress = state.accountAddressShortened,
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
    BoxWithConstraints(modifier = modifier.background(Brush.horizontalGradient(AccountGradientList[gradientIndex]))) {
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
                            FungibleTokenBottomSheetDetails(assetDetails, onCloseClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }
                            }, Modifier.fillMaxSize())
                        }
                        null -> {}
                    }
                }
            },
        ) {
//            val density = LocalDensity.current
//            val headerScrollState = rememberScrollableHeaderViewScrollState()
//            AnimatedVisibility(
//                visible = !isLoading,
//                enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
//                content = AccountContentWithScrollableHeader(
//                    swipeRefreshState = swipeRefreshState,
//                    onRefresh = onRefresh,
//                    headerScrollState = headerScrollState,
//                    accountName = accountName,
//                    onBackClick = onBackClick,
//                    onAccountPreferenceClick = onAccountPreferenceClick,
//                    accountAddress = accountAddress,
//                    walletFiatBalance = walletFiatBalance,
//                    onCopyAccountAddress = onCopyAccountAddress,
//                    onTransferClick = onTransferClick,
//                    xrdToken = xrdToken,
//                    fungibleTokens = fungibleTokens,
//                    nonFungibleTokens = nonFungibleTokens,
//                    onFungibleTokenClick = onFungibleTokenClick,
//                    onNftClick = onNftClick,
//                    density = density
//                )
//            )
            val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
            Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                Scaffold(
                    modifier = Modifier
                        .systemBarsPadding()
                        .fillMaxSize(),
                    topBar = {
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
                    },
                    backgroundColor = Color.Transparent
                ) { innerPadding ->
//                    val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = onRefresh)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
//                        .pullRefresh(pullRefreshState)
                    ) {
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
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    contentColor = RadixTheme.colors.gray1,
                    backgroundColor = RadixTheme.colors.defaultBackground,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
        AnimatedVisibility(modifier = Modifier.align(Alignment.BottomCenter), visible = !isLoading, enter = fadeIn()) {
            RadixSecondaryButton(
                text = stringResource(id = R.string.history),
                onClick = onHistoryClick,
                modifier = Modifier
                    .padding(bottom = RadixTheme.dimensions.paddingXXLarge)
                    .size(174.dp, 50.dp),
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
    modifier: Modifier = Modifier
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

@Composable
private fun NonFungibleTokenBottomSheetDetails(
    asset: NftCollectionUiModel,
    selectedNft: NftCollectionUiModel.NftItemUiModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = stringResource(id = R.string.empty),
            onBackClick = onCloseClick,
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = asset.iconUrl,
                placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                fallback = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.roundedRectMedium)
                    .clip(RadixTheme.shapes.roundedRectMedium)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            AssetMetadataRow(
                modifier = Modifier.fillMaxWidth(),
                key = stringResource(id = R.string.nft_id),
                value = selectedNft.id
            )
            selectedNft.nftsMetadata.forEach {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
                AssetMetadataRow(
                    modifier = Modifier.fillMaxWidth(),
                    key = it.first,
                    value = it.second
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun FungibleTokenBottomSheetDetails(
    token: TokenUiModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RadixCenteredTopAppBar(
            title = token.name.orEmpty(),
            onBackClick = onCloseClick,
            modifier = Modifier.fillMaxWidth(),
            contentColor = RadixTheme.colors.gray1,
            backIconType = BackIconType.Close
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RadixTheme.dimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = token.iconUrl,
                placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                fallback = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb())),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .background(RadixTheme.colors.gray3, RadixTheme.shapes.circle)
                    .clip(RadixTheme.shapes.circle)
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
            Text(
                text = token.tokenQuantityToDisplay,
                style = RadixTheme.typography.title,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
            Text(
                text = "\$44.21",
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray2
            )
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            token.description?.let { desc ->
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Text(
                    text = desc,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray2
                )
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
                Divider(Modifier.fillMaxWidth(), color = RadixTheme.colors.gray4)
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            }
            if (token.metadata.isNotEmpty()) {
                token.metadata.forEach { mapEntry ->
                    AssetMetadataRow(Modifier.fillMaxWidth(), mapEntry.key, mapEntry.value)
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun AssetMetadataRow(modifier: Modifier, key: String, value: String) {
    Row(
        modifier,
        horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Text(
            modifier = Modifier.weight(0.4f),
            text = key.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = RadixTheme.typography.body1Regular,
            color = RadixTheme.colors.gray2
        )
        Text(
            modifier = Modifier.weight(0.6f),
            text = value,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray1,
            textAlign = TextAlign.End
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
    isLoading: Boolean
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
    onTransferClick: () -> Unit
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
    isLoading: Boolean
) {
    Column(modifier = modifier) {
        val pagerState = rememberPagerState()
        val scope = rememberCoroutineScope()
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))
        ScrollableTabRow(
            modifier = Modifier.height(50.dp),
            selectedTabIndex = pagerState.currentPage,
            divider = {}, /* Disable the built-in divider */
            edgePadding = RadixTheme.dimensions.paddingLarge,
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
    BabylonWalletTheme {
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
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountContentDarkPreview() {
    BabylonWalletTheme(darkTheme = true) {
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
