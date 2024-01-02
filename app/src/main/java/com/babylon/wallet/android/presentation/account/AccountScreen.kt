@file:OptIn(ExperimentalMaterialApi::class)

package com.babylon.wallet.android.presentation.account

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.LocalDevBannerState
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.ThrottleIconButton
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewAction
import com.babylon.wallet.android.presentation.ui.composables.assets.assetsView
import com.babylon.wallet.android.presentation.ui.composables.assets.rememberAssetsViewState
import com.babylon.wallet.android.presentation.ui.composables.toText
import com.babylon.wallet.android.utils.openUrl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import rdx.works.profile.data.model.factorsources.FactorSource
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onAccountPreferenceClick: (address: String) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.FactorSourceID.FromHash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onFungibleResourceClick: (Resource.FungibleResource, Network.Account) -> Unit,
    onNonFungibleResourceClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onPoolUnitClick: (PoolUnit, Network.Account) -> Unit,
    onLSUClick: (LiquidStakeUnit, Network.Account) -> Unit,
    onTransferClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is AccountEvent.NavigateToMnemonicBackup -> onNavigateToMnemonicBackup(it.factorSourceId)
                is AccountEvent.NavigateToMnemonicRestore -> onNavigateToMnemonicRestore()
                is AccountEvent.OnFungibleClick -> onFungibleResourceClick(it.resource, it.account)
                is AccountEvent.OnNonFungibleClick -> onNonFungibleResourceClick(it.resource, it.item)
                is AccountEvent.OnPoolUnitClick -> onPoolUnitClick(it.poolUnit, it.account)
                is AccountEvent.OnLSUClick -> onLSUClick(it.liquidStakeUnit, it.account)
            }
        }
    }

    val devBannerState = LocalDevBannerState.current
    if (!devBannerState.isVisible) {
        SetStatusBarColor(useDarkIcons = false)
    }

    AccountScreenContent(
        modifier = modifier,
        state = state,
        onAccountPreferenceClick = { address ->
            onAccountPreferenceClick(address)
        },
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onTransferClick = onTransferClick,
        onMessageShown = viewModel::onMessageShown,
        onFungibleItemClicked = viewModel::onFungibleResourceClicked,
        onNonFungibleItemClicked = viewModel::onNonFungibleResourceClicked,
        onApplySecuritySettings = viewModel::onApplySecuritySettings,
        onPoolUnitClick = viewModel::onPoolUnitClicked,
        onLSUUnitClicked = viewModel::onLSUUnitClicked,
        onNextNFTsPageRequest = viewModel::onNextNftPageRequest,
        onStakesRequest = viewModel::onStakesRequest,
    )
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
    onAccountPreferenceClick: (address: String) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onTransferClick: (String) -> Unit,
    onMessageShown: () -> Unit,
    onFungibleItemClicked: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClicked: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onApplySecuritySettings: (SecurityPromptType) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit
) {
    val context = LocalContext.current

    val gradient = remember(state.accountWithAssets) {
        val appearanceId = state.accountWithAssets?.account?.appearanceID ?: 0
        AccountGradientList[appearanceId % AccountGradientList.size]
    }.toPersistentList()

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    val pullToRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        refreshingOffset = 116.dp
    )
    val lazyListState = rememberLazyListState()
    Box(
        modifier = modifier
            .pullRefresh(pullToRefreshState)
            .navigationBarsPadding()
            .background(Brush.horizontalGradient(gradient))
            .statusBarsPadding()
    ) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                RadixCenteredTopAppBar(
                    title = state.accountWithAssets?.account?.displayName.orEmpty(),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.white,
                    containerColor = Color.Transparent,
                    actions = {
                        // TODO revisit after compose update and remove if library update fixes the issue
                        // https://radixdlt.atlassian.net/browse/ABW-2504
                        ThrottleIconButton(
                            onClick = {
                                onAccountPreferenceClick(state.accountWithAssets?.account?.address.orEmpty())
                            },
                            thresholdMs = 1000L
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz
                                ),
                                tint = RadixTheme.colors.white,
                                contentDescription = "account settings"
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            floatingActionButtonPosition = FabPosition.Center,
            snackbarHost = {
                RadixSnackbarHost(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    hostState = snackBarHostState
                )
            }
        ) { innerPadding ->
            AssetsContent(
                modifier = Modifier.padding(innerPadding),
                state = state,
                lazyListState = lazyListState,
                onFungibleTokenClick = {
                    onFungibleItemClicked(it)
                },
                onNonFungibleItemClick = { nftCollection, nftItem ->
                    onNonFungibleItemClicked(nftCollection, nftItem)
                },
                gradient = gradient,
                onTransferClick = onTransferClick,
                onHistoryClick = {
                    state.historyDashboardUrl?.let { url ->
                        context.openUrl(url)
                    }
                },
                onApplySecuritySettings = onApplySecuritySettings,
                onPoolUnitClick = {
                    onPoolUnitClick(it)
                },
                onLSUUnitClicked = { lsu ->
                    onLSUUnitClicked(lsu)
                },
                onNextNFTsPageRequest = onNextNFTsPageRequest,
                onStakesRequest = onStakesRequest
            )
        }

        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.isRefreshing,
            state = pullToRefreshState,
            contentColor = RadixTheme.colors.gray1,
            backgroundColor = RadixTheme.colors.defaultBackground,
        )
    }
}

@Composable
fun AssetsContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    state: AccountUiState,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    gradient: ImmutableList<Color>,
    onTransferClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onApplySecuritySettings: (SecurityPromptType) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = RadixTheme.colors.gray5
    ) {
        val accountAddress = remember(state.accountWithAssets) {
            state.accountWithAssets?.account?.address.orEmpty()
        }

        var selectedTab by remember { mutableStateOf(ResourceTab.Tokens) }
        val collapsibleAssetsState = rememberAssetsViewState(assets = state.accountWithAssets?.assets)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = PaddingValues(bottom = RadixTheme.dimensions.paddingSemiLarge)
        ) {
            item {
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(gradient)
                            )
                            .padding(horizontal = RadixTheme.dimensions.paddingXLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingSemiLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActionableAddressView(
                            address = accountAddress,
                            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXLarge),
                            textStyle = RadixTheme.typography.body2HighImportance,
                            textColor = RadixTheme.colors.white
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                            visible = state.isTransferEnabled,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = RadixTheme.dimensions.paddingXSmall),
                                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                            ) {
                                HistoryButton(
                                    modifier = Modifier.weight(1f),
                                    onHistoryClick = onHistoryClick
                                )
                                TransferButton(
                                    modifier = Modifier.weight(1f),
                                    accountAddress = accountAddress,
                                    onTransferClick = onTransferClick
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                            visible = state.visiblePrompt != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ApplySecuritySettingsLabel(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    state.visiblePrompt?.let(onApplySecuritySettings)
                                },
                                text = state.visiblePrompt?.toText().orEmpty()
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(RadixTheme.dimensions.paddingSemiLarge)
                            .background(
                                color = RadixTheme.colors.gray5,
                                shape = RadixTheme.shapes.roundedRectTopDefault
                            )
                    )
                }
            }

            assetsView(
                assets = state.accountWithAssets?.assets,
                epoch = state.epoch,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                collapsibleAssetsState = collapsibleAssetsState,
                action = AssetsViewAction.Click(
                    onFungibleClick = onFungibleTokenClick,
                    onNonFungibleItemClick = onNonFungibleItemClick,
                    onLSUClick = onLSUUnitClicked,
                    onPoolUnitClick = onPoolUnitClick,
                    onNextNFtsPageRequest = onNextNFTsPageRequest,
                    onStakesRequest = onStakesRequest
                )
            )
        }
    }
}

@Composable
private fun TransferButton(
    modifier: Modifier = Modifier,
    accountAddress: String,
    onTransferClick: (String) -> Unit
) {
    RadixSecondaryButton(
        modifier = modifier,
        text = stringResource(id = R.string.account_transfer),
        onClick = { onTransferClick(accountAddress) },
        containerColor = RadixTheme.colors.white.copy(alpha = 0.2f),
        contentColor = RadixTheme.colors.white,
        shape = RadixTheme.shapes.circle,
        leadingContent = {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_transfer),
                tint = RadixTheme.colors.white,
                contentDescription = null
            )
        }
    )
}

@Composable
private fun HistoryButton(
    modifier: Modifier = Modifier,
    onHistoryClick: () -> Unit
) {
    RadixSecondaryButton(
        text = stringResource(id = R.string.common_history),
        onClick = onHistoryClick,
        modifier = modifier,
        containerColor = RadixTheme.colors.white.copy(alpha = 0.2f),
        contentColor = RadixTheme.colors.white,
        shape = RadixTheme.shapes.circle,
        leadingContent = {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_watch_later),
                tint = RadixTheme.colors.white,
                contentDescription = null
            )
        },
        trailingContent = {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_link_out),
                tint = RadixTheme.colors.white.copy(alpha = 0.5f),
                contentDescription = null
            )
        }
    )
}

@Preview
@Composable
fun AccountContentPreview() {
    RadixWalletTheme {
        with(SampleDataProvider()) {
            AccountScreenContent(
                state = AccountUiState(
                    accountWithAssets = AccountWithAssets(
                        account = sampleAccount("acount_rdx_abcde"),
                        assets = Assets(
                            fungibles = sampleFungibleResources(),
                            nonFungibles = listOf(),
                            poolUnits = listOf(),
                            validatorsWithStakes = emptyList()
                        ),
                    )
                ),
                onAccountPreferenceClick = { _ -> },
                onBackClick = {},
                onRefresh = {},
                onTransferClick = {},
                onMessageShown = {},
                onFungibleItemClicked = {},
                onNonFungibleItemClicked = { _, _ -> },
                onApplySecuritySettings = {},
                onPoolUnitClick = {},
                onLSUUnitClicked = {},
                onNextNFTsPageRequest = {},
                onStakesRequest = {}
            )
        }
    }
}
