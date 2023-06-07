@file:OptIn(ExperimentalFoundationApi::class,)

package com.babylon.wallet.android.presentation.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.AccountGradientList
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.presentation.account.composable.FungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.account.composable.NonFungibleTokenBottomSheetDetails
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.transfer.assets.ResourcesTabs
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourcesColumn
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourcesColumn
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.factorsources.FactorSource

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onAccountPreferenceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.ID) -> Unit,
    onTransferClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetStatusBarColor(color = Color.Transparent, useDarkIcons = !isSystemInDarkTheme())
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is AccountEvent.NavigateToMnemonicBackup -> {
                    onNavigateToMnemonicBackup(it.factorSourceId)
                }
            }
        }
    }
    AccountScreenContent(
        state = state,
        onAccountPreferenceClick = {
            onAccountPreferenceClick(it)
        },
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onHistoryClick = {},
        onTransferClick = onTransferClick,
        onFungibleResourceClicked = viewModel::onFungibleResourceClicked,
        onNonFungibleItemClicked = viewModel::onNonFungibleResourceClicked,
        modifier = modifier,
        onApplySecuritySettings = viewModel::onApplySecuritySettings
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onAccountPreferenceClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onHistoryClick: () -> Unit,
    onTransferClick: (String) -> Unit,
    onFungibleResourceClicked: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClicked: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    val gradient = remember(state.accountWithResources) {
        val appearanceId = state.accountWithResources?.account?.appearanceID ?: 0
        AccountGradientList[appearanceId % AccountGradientList.size]
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Brush.horizontalGradient(gradient))
    ) {
        val bottomSheetState =
            rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
        val scope = rememberCoroutineScope()
        val sheetHeight = maxHeight * 0.9f
        BackHandler {
            if (bottomSheetState.isVisible) {
                scope.launch {
                    bottomSheetState.hide()
                }
            } else {
                onBackClick()
            }
        }
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
                    when (state.selectedResource) {
                        is SelectedResource.SelectedNonFungibleResource -> {
                            NonFungibleTokenBottomSheetDetails(
                                modifier = Modifier.fillMaxSize(),
                                item = state.selectedResource.item,
                                onCloseClick = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                }
                            )
                        }
                        is SelectedResource.SelectedFungibleResource -> {
                            FungibleTokenBottomSheetDetails(
                                modifier = Modifier.fillMaxSize(),
                                fungible = state.selectedResource.fungible,
                                onCloseClick = {
                                    scope.launch {
                                        bottomSheetState.hide()
                                    }
                                }
                            )
                        }
                        else -> {}
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
                            title = state.accountWithResources?.account?.displayName.orEmpty(),
                            onBackClick = onBackClick,
                            containerColor = Color.Transparent,
                            contentColor = RadixTheme.colors.white,
                            actions = {
                                IconButton(
                                    onClick = {
                                        onAccountPreferenceClick(state.accountWithResources?.account?.address.orEmpty())
                                    }
                                ) {
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
                    val pullToRefreshState = rememberSwipeRefreshState(isRefreshing = state.isRefreshing)
                    SwipeRefresh(
                        modifier = Modifier.fillMaxSize(),
                        state = pullToRefreshState,
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
                                modifier = Modifier.fillMaxSize(),
                                state = state,
                                onTransferClick = onTransferClick,
                                onFungibleResourceClicked = {
                                    onFungibleResourceClicked(it)
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                onNonFungibleItemClicked = { nftCollection, nftItem ->
                                    onNonFungibleItemClicked(nftCollection, nftItem)
                                    scope.launch {
                                        bottomSheetState.show()
                                    }
                                },
                                onApplySecuritySettings = onApplySecuritySettings
                            )
//                        PullRefreshIndicator(
//                            refreshing = isRefreshing,
//                            state = pullRefreshState,
//                            contentColor = RadixTheme.colors.gray1,
//                            backgroundColor = RadixTheme.colors.defaultBackground,
//                            modifier = Modifier.align(Alignment.TopCenter)
//                        )
                            if (state.isLoading) {
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
        text = stringResource(id = R.string.common_history),
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
private fun AccountContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onTransferClick: (String) -> Unit,
    onFungibleResourceClicked: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClicked: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onApplySecuritySettings: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AccountSummaryContent(
            accountAddress = state.accountWithResources?.account?.address.orEmpty(),
            showSecurityPrompt = state.isSecurityPromptVisible,
            onTransferClick = onTransferClick,
            onApplySecuritySettings = onApplySecuritySettings,
        )
        AssetsContent(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = RadixTheme.colors.gray5,
                    shape = RadixTheme.shapes.roundedRectTopDefault
                )
                .clip(RadixTheme.shapes.roundedRectTopDefault),
            resources = state.accountWithResources?.resources,
            isLoading = state.isLoading,
            onFungibleTokenClick = onFungibleResourceClicked,
            onNonFungibleItemClick = onNonFungibleItemClicked
        )
    }
}

@Composable
private fun AccountSummaryContent(
    modifier: Modifier = Modifier,
    accountAddress: String,
    walletFiatBalance: String? = null,
    onTransferClick: (String) -> Unit,
    showSecurityPrompt: Boolean,
    onApplySecuritySettings: () -> Unit
) {
    Column(
        modifier = modifier.padding(bottom = RadixTheme.dimensions.paddingDefault),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionableAddressView(
            address = accountAddress,
            textStyle = RadixTheme.typography.body2HighImportance,
            textColor = RadixTheme.colors.white
        )

        walletFiatBalance?.let { value ->
            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingLarge))
            WalletBalanceView(
                currencySignValue = "$",
                amount = value,
                hidden = false,
                balanceClicked = {},
                contentColor = RadixTheme.colors.white
            )
        }

        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingXLarge))
        RadixSecondaryButton(
            text = stringResource(id = R.string.account_transfer),
            onClick = { onTransferClick(accountAddress) },
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

        AnimatedVisibility(
            modifier = Modifier
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(top = RadixTheme.dimensions.paddingLarge),
            visible = showSecurityPrompt,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ApplySecuritySettingsLabel(
                modifier = Modifier.fillMaxWidth(),
                onClick = onApplySecuritySettings,
                text = stringResource(id = R.string.homePage_applySecuritySettings)
            )
        }
    }
}

@Composable
fun AssetsContent(
    modifier: Modifier = Modifier,
    resources: Resources?,
    isLoading: Boolean,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val pagerState = rememberPagerState()
        var selectedTab by remember { mutableStateOf(ResourceTab.Tokens) }

        Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingLarge))

        ResourcesTabs(
            pagerState = pagerState,
            selectedTab = selectedTab,
            onTabSelected = {
                selectedTab = it
            }
        )

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            pageCount = ResourceTab.values().count(),
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            val tab = remember(page) {
                ResourceTab.values().find { it.ordinal == page } ?: ResourceTab.Tokens
            }

            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn(),
                content = {
                    when (tab) {
                        ResourceTab.Tokens -> FungibleResourcesColumn(
                            modifier = Modifier.fillMaxSize(),
                            resources = resources
                        ) { _, item ->
                            FungibleResourceItem(
                                modifier = Modifier
                                    .height(83.dp)
                                    .clickable {
                                        onFungibleTokenClick(item)
                                    },
                                resource = item
                            )
                        }
                        ResourceTab.Nfts -> NonFungibleResourcesColumn(
                            resources = resources,
                            modifier = Modifier.fillMaxSize(),
                        ) { collection, item ->
                            NonFungibleResourceItem(
                                modifier = Modifier
                                    .padding(RadixTheme.dimensions.paddingDefault)
                                    .clickable {
                                        onNonFungibleItemClick(collection, item)
                                    },
                                item = item
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
                state = AccountUiState(
                    accountWithResources = AccountWithResources(
                        account = sampleAccount("acount_rdx_abcde"),
                        resources = Resources(
                            fungibleResources = sampleFungibleResources(),
                            nonFungibleResources = listOf()
                        ),
                    )
                ),
                onAccountPreferenceClick = {},
                onBackClick = {},
                onRefresh = {},
                onHistoryClick = {},
                onTransferClick = {},
                onFungibleResourceClicked = {},
                onNonFungibleItemClicked = { _, _ -> },
                onApplySecuritySettings = {}
            )
        }
    }
}
