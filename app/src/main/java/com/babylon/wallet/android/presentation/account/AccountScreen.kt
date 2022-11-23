package com.babylon.wallet.android.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
    val swipeRefreshState = rememberSwipeRefreshState(viewModel.isRefreshing.collectAsStateWithLifecycle().value)

    Scaffold(
        modifier = modifier.systemBarsPadding(),
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
                            onCopyAccountAddressClick = viewModel::onCopyAccountAddress,
                            accountAddress = state.accountAddress,
                            xrdToken = state.xrdToken,
                            fungibleTokens = state.fungibleTokens,
                            nonFungibleTokens = state.nonFungibleTokens,
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
    onCopyAccountAddressClick: (String) -> Unit,
    accountAddress: String,
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftUiModel>,
    modifier: Modifier = Modifier
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

        AssetTypeTabsRow()

        AssetsContent(
            xrdToken, fungibleTokens, nonFungibleTokens
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
    HorizontalPager(
        modifier = modifier,
        count = 2, // TODO
        state = rememberPagerState(),
        userScrollEnabled = false
    ) { page ->
        when (page) {
            0 -> {
                ListOfTokensContent(
                    tokenItems = fungibleTokens,
                    xrdTokenUi = xrdToken,
                    modifier = Modifier.heightIn(min = 200.dp, max = 600.dp),
                )
            }
            1 -> {
                val collapsedState = remember(nonFungibleTokens) { nonFungibleTokens.map { true }.toMutableStateList() }
                CollapsableLazyColumn(
                    collapsedState = collapsedState,
                    sections = nonFungibleTokens,
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
private fun AssetTypeTabsRow() {
    val pagerState = rememberPagerState()
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
                onCopyAccountAddressClick = {},
                accountAddress = randomTokenAddress(),
                xrdToken = sampleFungibleTokens().first().toTokenUiModel(),
                fungibleTokens = sampleFungibleTokens().map { it.toTokenUiModel() }.toPersistentList(),
                nonFungibleTokens = persistentListOf(),
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
                onCopyAccountAddressClick = {},
                modifier = Modifier,
                nonFungibleTokens = persistentListOf(),
                fungibleTokens = sampleFungibleTokens().map { it.toTokenUiModel() }.toPersistentList(),
                accountAddress = randomTokenAddress(),
                xrdToken = sampleFungibleTokens().first().toTokenUiModel()
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
        AssetTypeTabsRow()
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
