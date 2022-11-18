package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Button
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
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountResources
import com.babylon.wallet.android.presentation.model.toNftUiModel
import com.babylon.wallet.android.presentation.model.toTokenUi
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.composables.CollapsableLazyColumn
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.theme.BabylonWalletTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalPagerApi::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    accountName: String,
    onMenuItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val state = viewModel.accountUiState.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState(initialPage = 0)

    val tokenLazyListState = rememberLazyListState()
    val swipeRefreshState = rememberSwipeRefreshState(viewModel.isRefreshing.collectAsStateWithLifecycle().value)

    Scaffold(
        modifier = modifier,
        topBar = {
            AccountTopAppBar(
                accountName = accountName,
                onMenuItemClick = onMenuItemClick,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->

        when (state) {
            AccountUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary
                    )
                }
            }
            is AccountUiState.Loaded -> {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refresh() },
                    indicatorPadding = innerPadding,
                    refreshTriggerDistance = 100.dp,
                    content = {
                        AccountContent(
                            account = state.account,
                            pagerState = pagerState,
                            onCopyAccountAddressClick = viewModel::onCopyAccountAddress,
                            tokenLazyListState = tokenLazyListState,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                )
            }
        }
    }
}

@ExperimentalPagerApi
@Composable
private fun AccountContent(
    account: AccountResources,
    pagerState: PagerState,
    onCopyAccountAddressClick: (String) -> Unit,
    tokenLazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp) // temp space value
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAddressView(
                address = account.address,
                onCopyAccountAddressClick = onCopyAccountAddressClick,
                modifier = Modifier.weight(1f, false)
            )
        }

        WalletBalanceView(
            currencySignValue = "$",
            amount = "10",
            hidden = false
        ) {
        }

        Button(onClick = { /*TODO*/ }) {
            Text(text = stringResource(id = R.string.account_transfer_button_title))
        }

        AssetTypeTabsRow(pagerState = pagerState)

        AssetsContent(
            pagerState = pagerState,
            tokenLazyListState = tokenLazyListState,
            account = account
        )
    }
}

@ExperimentalPagerApi
@Composable
fun AssetsContent(
    pagerState: PagerState,
    account: AccountResources,
    tokenLazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        modifier = modifier,
        count = 2, // TODO
        state = pagerState,
        userScrollEnabled = false
    ) { page ->
        when (page) {
            0 -> {
                val xrdToken = if (account.hasXrdToken()) account.fungibleTokens[0] else null
                val tokensToShow = if (account.hasXrdToken()) {
                    account.fungibleTokens.subList(1, account.fungibleTokens.size)
                } else {
                    account.fungibleTokens
                }

                ListOfTokensContent(
                    tokenItems = tokensToShow.map { it.toTokenUi() },
                    lazyListState = tokenLazyListState,
                    xrdTokenUi = xrdToken?.toTokenUi(),
                    modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
                )
            }
            1 -> {
                val nftSections = account.nonFungibleTokens.map { it.toNftUiModel() }
                val collapsedState = remember(nftSections) { nftSections.map { true }.toMutableStateList() }
                CollapsableLazyColumn(
                    collapsedState = collapsedState,
                    sections = nftSections,
                    modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountTopAppBar(
    accountName: String,
    onMenuItemClick: () -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "navigate back"
                )
            }
        },
        title = {
            Text(
                text = accountName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(onClick = { onMenuItemClick() }) {
                BadgedBox(
                    badge = { Badge() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "account settings"
                    )
                }
            }
        },
        elevation = 0.dp
    )
}

@ExperimentalPagerApi
@Composable
private fun AssetTypeTabsRow(
    pagerState: PagerState
) {
    val scope = rememberCoroutineScope()
    ScrollableTabRow(
        selectedTabIndex = pagerState.currentPage,
        divider = {}, /* Disable the built-in divider */
        edgePadding = 24.dp,
        indicator = emptyTabIndicator
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
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
                )
            }
        }
    }
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

@OptIn(ExperimentalPagerApi::class)
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountContentPreview() {
    BabylonWalletTheme {
        with(SampleDataProvider()) {
            AccountContent(
                account = sampleAccountResource(),
                pagerState = PagerState(currentPage = 0),
                onCopyAccountAddressClick = {},
                tokenLazyListState = LazyListState(),
                modifier = Modifier
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AccountContentPreview2() {
    BabylonWalletTheme {
        with(SampleDataProvider()) {
            AccountContent(
                account = sampleAccountResource(),
                pagerState = PagerState(currentPage = 1),
                onCopyAccountAddressClick = {},
                tokenLazyListState = LazyListState(),
                modifier = Modifier
            )
        }
    }
}

@ExperimentalPagerApi
@Preview(showBackground = true)
@Preview("large font", fontScale = 2f, showBackground = true)
@Composable
fun AssetTabRowPreview() {
    BabylonWalletTheme {
        val pagerState = rememberPagerState(initialPage = 0)
        AssetTypeTabsRow(
            pagerState = pagerState
        )
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
