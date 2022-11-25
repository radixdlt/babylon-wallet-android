package com.babylon.wallet.android.presentation.account

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
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
import com.babylon.wallet.android.presentation.model.NftUiModel
import com.babylon.wallet.android.presentation.model.TokenUiModel
import com.babylon.wallet.android.presentation.model.toTokenUiModel
import com.babylon.wallet.android.presentation.ui.composables.AccountAddressView
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.babylon.wallet.android.presentation.ui.composables.NftTokenList
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
        onFungibleTokenClick = viewModel::onFungibleTokenClick,
        state.tokenDetails,
        modifier = modifier
    )
}

@Composable
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterialApi::class)
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
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    tokenDetails: TokenUiModel?,
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
                    FungibleTokenBottomSheetDetails(tokenDetails, onCloseClick = {
                        scope.launch {
                            bottomSheetState.hide()
                        }
                    }, Modifier.fillMaxSize())
                }
            },
        ) {
            Scaffold(
                modifier = Modifier
                    .systemBarsPadding()
                    .fillMaxSize(),
                topBar = {
                    RadixCenteredTopAppBar(
                        title = accountName,
                        onBackClick = onBackClick,
                        actions = {
                            IconButton(onClick = { onMenuItemClick() }) {
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
                        modifier = Modifier.fillMaxSize(),
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
                                onTransferClick = onTransferClick,
                                onFungibleTokenClick = {
                                    onFungibleTokenClick(it)
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                }
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

@Composable
private fun FungibleTokenBottomSheetDetails(
    tokenDetails: TokenUiModel?,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tokenDetails?.let { token ->
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
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingMedium)
                        ) {
                            Text(
                                modifier = Modifier.weight(0.4f),
                                text = mapEntry.key.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                },
                                style = RadixTheme.typography.body1Regular,
                                color = RadixTheme.colors.gray2
                            )
                            Text(
                                modifier = Modifier.weight(0.6f),
                                text = mapEntry.value,
                                style = RadixTheme.typography.body1HighImportance,
                                color = RadixTheme.colors.gray1,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
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
    onTransferClick: () -> Unit,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
    modifier: Modifier = Modifier
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
        WalletBalanceView(
            currencySignValue = "$",
            amount = "10",
            hidden = false, balanceClicked = {}, contentColor = RadixTheme.colors.white
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
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
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
        AssetsContent(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = RadixTheme.colors.gray5, shape = RadixTheme.shapes.roundedRectTopDefault
                )
                .clip(RadixTheme.shapes.roundedRectTopDefault),
            xrdToken = xrdToken,
            fungibleTokens = fungibleTokens,
            nonFungibleTokens = nonFungibleTokens,
            onFungibleTokenClick = onFungibleTokenClick
        )
    }
}

@ExperimentalPagerApi
@Composable
fun AssetsContent(
    xrdToken: TokenUiModel?,
    fungibleTokens: ImmutableList<TokenUiModel>,
    nonFungibleTokens: ImmutableList<NftUiModel>,
    onFungibleTokenClick: (TokenUiModel) -> Unit,
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
            backgroundColor = Color.Transparent
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
                        modifier = Modifier
                            .padding(vertical = RadixTheme.dimensions.paddingMedium)
                            .height(40.dp)
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
                        modifier = Modifier.fillMaxSize(),
                        onFungibleTokenClick = onFungibleTokenClick
                    )
                }
                AssetTypeTab.NTF_TAB -> {
                    val collapsedState =
                        remember(nonFungibleTokens) { nonFungibleTokens.map { true }.toMutableStateList() }
                    NftTokenList(
                        collapsedState = collapsedState,
                        item = nonFungibleTokens,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
            selected -> RadixTheme.colors.gray1
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> RadixTheme.colors.white
            else -> RadixTheme.colors.gray1
        },
        shape = RadixTheme.shapes.circle,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                    ),
                style = RadixTheme.typography.body1HighImportance
            )
        }
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
                onFungibleTokenClick = {},
                tokenDetails = null,
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
                onFungibleTokenClick = {},
                tokenDetails = null,
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
