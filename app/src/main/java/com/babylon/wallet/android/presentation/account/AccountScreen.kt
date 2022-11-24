package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.BabylonWalletTheme
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.presentation.model.NftUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.composables.CollapsableLazyColumn
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    accountName: String,
    onMenuItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state by viewModel.accountUiState.collectAsStateWithLifecycle()
    SetStatusBarColor(color = Color.Transparent, useDarkIcons = !isSystemInDarkTheme())
    AccountScreenContent(
        accountName = accountName,
        onMenuItemClick = onMenuItemClick,
        onBackClick = onBackClick,
        isLoading = state.isLoading,
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        accountAddress = state.accountAddress,
        xrdToken = state.xrdToken,
        fungibleTokens = state.fungibleTokens,
        nonFungibleTokens = state.nonFungibleTokens,
        onCopyAccountAddress = viewModel::onCopyAccountAddress,
        gradientIndex = state.gradientIndex,
        onHistoryClick = {},
        onTransferClick = {},
        modifier = modifier
    )
}

@Composable
@OptIn(ExperimentalPagerApi::class)
private fun AccountScreenContent(
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    accountAddress: String,
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftUiModel>,
    onCopyAccountAddress: (String) -> Unit,
    gradientIndex: Int,
    onHistoryClick: () -> Unit,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Brush.horizontalGradient(AccountGradientList[gradientIndex]))) {
        Scaffold(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            topBar = {
                AccountTopAppBar(
                    accountName = accountName,
                    onMenuItemClick = onMenuItemClick,
                    onBackClick = onBackClick
                )
            },
            backgroundColor = Color.Transparent
        ) { innerPadding ->
            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = RadixTheme.colors.gray1
                    )
                }
            } else {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = onRefresh,
                    indicatorPadding = innerPadding,
                    refreshTriggerDistance = 100.dp,
                    content = {
                        AccountContent(
                            onCopyAccountAddressClick = onCopyAccountAddress,
                            accountAddress = accountAddress,
                            xrdToken = xrdToken,
                            fungibleTokens = fungibleTokens,
                            nonFungibleTokens = nonFungibleTokens,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            onTransferClick = onTransferClick
                        )
                    }
                )
            }
        }
        RadixSecondaryButton(
            modifier = Modifier
                .padding(bottom = RadixTheme.dimensions.paddingXXLarge)
                .size(174.dp, 50.dp)
                .align(Alignment.BottomCenter),
            text = stringResource(id = R.string.history),
            onClick = onHistoryClick,
            contentColor = RadixTheme.colors.white,
            containerColor = RadixTheme.colors.gray2,
            shape = RadixTheme.shapes.circle,
            icon = {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_watch_later),
                    tint = RadixTheme.colors.white,
                    contentDescription = null
                )
            }
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
    nonFungibleTokens: ImmutableList<NftUiModel>,
    modifier: Modifier = Modifier,
    onTransferClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingSmall) // temp space value
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAddressView(
                address = accountAddress,
                onCopyAccountAddressClick = onCopyAccountAddressClick,
                modifier = Modifier.weight(1f, false),
                contentColor = RadixTheme.colors.white
            )
        }

        WalletBalanceView(
            currencySignValue = "$",
            amount = "10",
            hidden = false, balanceClicked = {}, contentColor = RadixTheme.colors.white
        )
        RadixSecondaryButton(
            text = stringResource(id = R.string.account_transfer_button_title),
            onClick = onTransferClick,
            contentColor = RadixTheme.colors.white,
            containerColor = RadixTheme.colors.white.copy(alpha = 0.2f),
            shape = RadixTheme.shapes.circle,
            icon = {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_transfer),
                    tint = RadixTheme.colors.white,
                    contentDescription = null
                )
            }
        )

        AssetsContent(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = RadixTheme.colors.defaultBackground,
                    shape = RadixTheme.shapes.roundedRectTopDefault
                )
                .clip(RadixTheme.shapes.roundedRectTopDefault),
            xrdToken = xrdToken, fungibleTokens = fungibleTokens, nonFungibleTokens = nonFungibleTokens
        )
    }
}

@ExperimentalPagerApi
@Composable
fun AssetsContent(
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftUiModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val pagerState = rememberPagerState()
        val scope = rememberCoroutineScope()
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            divider = {}, /* Disable the built-in divider */
            edgePadding = RadixTheme.dimensions.paddingLarge,
            indicator = emptyTabIndicator,
        ) {
            AssetTypeTab.values().forEachIndexed { index, assetTypeTab ->
                Tab(
                    selected = index == pagerState.currentPage,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                ) {
                    ChoiceChipContent(
                        text = stringResource(id = assetTypeTab.stringId),
                        selected = index == pagerState.currentPage,
                        modifier = Modifier.padding(
                            horizontal = RadixTheme.dimensions.paddingXSmall,
                            vertical = RadixTheme.dimensions.paddingDefault
                        )
                    )
                }
            }
        }
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            count = AssetTypeTab.values().size,
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            when (AssetTypeTab.values()[page]) {
                AssetTypeTab.TOKEN_TAB -> {
                    ListOfTokensContent(
                        tokenItems = fungibleTokens,
                        xrdTokenUi = xrdToken,
                        modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
                    )
                }
                AssetTypeTab.NTF_TAB -> {
                    val collapsedState =
                        remember(nonFungibleTokens) { nonFungibleTokens.map { true }.toMutableStateList() }
                    CollapsableLazyColumn(
                        collapsedState = collapsedState,
                        sections = nonFungibleTokens,
                        modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTopAppBar(
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_arrow_back),
                    tint = RadixTheme.colors.white,
                    contentDescription = "navigate back"
                )
            }
        },
        title = {
            Text(
                text = accountName,
                style = RadixTheme.typography.body1Header,
                color = RadixTheme.colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = { onMenuItemClick() }) {
                Icon(
                    painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz),
                    tint = RadixTheme.colors.white,
                    contentDescription = "account settings"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun ChoiceChipContent(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.onSurface
            else -> MaterialTheme.colors.primary
        },
        contentColor = when {
            selected -> MaterialTheme.colors.primary
            else -> MaterialTheme.colors.onPrimary
        },
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private val emptyTabIndicator: @Composable (List<TabPosition>) -> Unit = {}

@Preview
@Composable
fun AccountContentPreview() {
    BabylonWalletTheme {
        with(SampleDataProvider()) {
            AccountScreenContent(
                accountName = randomTokenAddress(),
                onMenuItemClick = {},
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
                onMenuItemClick = {},
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
                modifier = Modifier
            )
        }
    }
}

@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun ChoiceChipContentPreview() {
    BabylonWalletTheme {
        ChoiceChipContent(
            text = "Tokens",
            selected = true,
            modifier = Modifier
        )
    }
}
