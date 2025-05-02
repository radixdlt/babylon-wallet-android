@file:OptIn(ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.account

import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.edgeToEdge
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.designsystem.theme.plus
import com.babylon.wallet.android.domain.model.assets.AccountWithAssets
import com.babylon.wallet.android.domain.model.locker.AccountLockerDeposit
import com.babylon.wallet.android.domain.usecases.securityproblems.SecurityPromptType
import com.babylon.wallet.android.presentation.account.AccountViewModel.Event
import com.babylon.wallet.android.presentation.account.AccountViewModel.State
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.AccountPromptLabel
import com.babylon.wallet.android.presentation.ui.composables.LocalDevBannerState
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewAction
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewData
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceViewToggle
import com.babylon.wallet.android.presentation.ui.composables.assets.assetsView
import com.babylon.wallet.android.presentation.ui.composables.dAppDisplayName
import com.babylon.wallet.android.presentation.ui.composables.statusBarsAndBanner
import com.babylon.wallet.android.presentation.ui.composables.toText
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.AppearanceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.samples.sampleMainnet
import rdx.works.core.domain.assets.Assets
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.SupportedCurrency
import rdx.works.core.domain.resources.Resource

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onAccountPreferenceClick: (address: AccountAddress) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onFungibleResourceClick: (Resource.FungibleResource, Account) -> Unit,
    onNonFungibleResourceClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Account) -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is Event.NavigateToSecurityCenter -> onNavigateToSecurityCenter()
                is Event.OnFungibleClick -> onFungibleResourceClick(it.resource, it.account)
                is Event.OnNonFungibleClick -> onNonFungibleResourceClick(it.resource, it.item, it.account)
            }
        }
    }

    SetStatusBarColors()

    AccountScreenContent(
        modifier = modifier,
        state = state,
        onShowHideBalanceToggle = viewModel::onShowHideBalanceToggle,
        onAccountPreferenceClick = { address ->
            onAccountPreferenceClick(address)
        },
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onTransferClick = onTransferClick,
        onMessageShown = viewModel::onMessageShown,
        onFungibleItemClicked = viewModel::onFungibleResourceClicked,
        onNonFungibleItemClicked = viewModel::onNonFungibleResourceClicked,
        onApplySecuritySettingsClick = viewModel::onApplySecuritySettingsClick,
        onLockerDepositClick = viewModel::onLockerDepositClick,
        onPoolUnitClick = viewModel::onPoolUnitClicked,
        onLSUUnitClicked = viewModel::onLSUUnitClicked,
        onNextNFTsPageRequest = viewModel::onNextNftPageRequest,
        onStakesRequest = viewModel::onStakesRequest,
        onClaimClick = viewModel::onClaimClick,
        onTabClick = viewModel::onTabSelected,
        onCollectionClick = viewModel::onCollectionToggle,
        onHistoryClick = onHistoryClick,
        onInfoClick = onInfoClick
    )
}

@Composable
private fun SetStatusBarColors() {
    val isDevBannerVisible = LocalDevBannerState.current.isVisible
    val isDarkThemeEnabled = RadixTheme.config.isDarkTheme
    val context = LocalContext.current
    DisposableEffect(
        isDevBannerVisible,
        isDarkThemeEnabled
    ) {
        (context as ComponentActivity).edgeToEdge(
            isDarkThemeEnabled = isDarkThemeEnabled,
            forceDarkStatusBar = true
        )

        // When screen is closed, then reset the theme
        onDispose {
            (context as ComponentActivity).edgeToEdge(
                isDarkThemeEnabled = isDarkThemeEnabled,
                forceDarkStatusBar = isDevBannerVisible
            )
        }
    }
}

@Composable
private fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: State,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onAccountPreferenceClick: (address: AccountAddress) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onMessageShown: () -> Unit,
    onTabClick: (AssetsTab) -> Unit,
    onCollectionClick: (String) -> Unit,
    onFungibleItemClicked: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClicked: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onApplySecuritySettingsClick: () -> Unit,
    onLockerDepositClick: (AccountLockerDeposit) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
    onClaimClick: (List<StakeClaim>) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    val gradient = (state.accountWithAssets?.account?.appearanceId ?: AppearanceId(0u)).gradient()

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    val lazyListState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = modifier
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            )
    ) {
        Scaffold(
            modifier = Modifier.background(gradient),
            topBar = {
                RadixCenteredTopAppBar(
                    title = state.accountWithAssets?.account?.displayName?.value.orEmpty(),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.white,
                    containerColor = Color.Transparent,
                    actions = {
                        IconButton(
                            onClick = {
                                state.accountWithAssets?.account?.let {
                                    onAccountPreferenceClick(it.address)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz
                                ),
                                tint = RadixTheme.colors.white,
                                contentDescription = "account settings"
                            )
                        }
                    },
                    windowInsets = WindowInsets.statusBarsAndBanner
                )
            },
            containerColor = Color.Transparent,
            floatingActionButtonPosition = FabPosition.Center,
            snackbarHost = {
                RadixSnackbarHost(
                    modifier = Modifier.padding(RadixTheme.dimensions.paddingDefault),
                    hostState = snackBarHostState
                )
            },
            contentWindowInsets = WindowInsets.statusBarsAndBanner
        ) { innerPadding ->
            AssetsContent(
                modifier = Modifier.padding(innerPadding),
                state = state,
                lazyListState = lazyListState,
                onShowHideBalanceToggle = onShowHideBalanceToggle,
                onFungibleTokenClick = {
                    onFungibleItemClicked(it)
                },
                onNonFungibleItemClick = { nftCollection, nftItem ->
                    onNonFungibleItemClicked(nftCollection, nftItem)
                },
                gradient = gradient,
                onTransferClick = onTransferClick,
                onHistoryClick = onHistoryClick,
                onApplySecuritySettingsClick = onApplySecuritySettingsClick,
                onLockerDepositClick = onLockerDepositClick,
                onPoolUnitClick = {
                    onPoolUnitClick(it)
                },
                onLSUUnitClicked = { lsu ->
                    onLSUUnitClicked(lsu)
                },
                onNextNFTsPageRequest = onNextNFTsPageRequest,
                onStakesRequest = onStakesRequest,
                onClaimClick = onClaimClick,
                onTabClick = onTabClick,
                onCollectionClick = onCollectionClick,
                onInfoClick = onInfoClick
            )
        }

        PullToRefreshDefaults.Indicator(
            modifier = Modifier
                .align(Alignment.TopCenter),
            state = pullToRefreshState,
            isRefreshing = state.isRefreshing,
            color = RadixTheme.colors.icon,
            containerColor = RadixTheme.colors.background,
            threshold = WindowInsets.statusBarsAndBanner
                .asPaddingValues()
                .calculateTopPadding() + PullToRefreshDefaults.PositionalThreshold
        )
    }
}

@Composable
fun AssetsContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    state: State,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onTabClick: (AssetsTab) -> Unit,
    onCollectionClick: (String) -> Unit,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    gradient: Brush,
    onTransferClick: (AccountAddress) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onApplySecuritySettingsClick: () -> Unit,
    onLockerDepositClick: (AccountLockerDeposit) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
    onClaimClick: (List<StakeClaim>) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    Surface(
        modifier = modifier,
        color = RadixTheme.colors.backgroundSecondary
    ) {
        val accountAddress = remember(state.accountWithAssets) {
            state.accountWithAssets?.account?.address
        }

        val assetsViewData = remember(state.accountWithAssets?.assets, state.assetsWithPrices, state.epoch) {
            AssetsViewData.from(
                assets = state.accountWithAssets?.assets,
                prices = state.assetsWithPrices,
                epoch = state.epoch
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues().plus(
                other = PaddingValues(bottom = RadixTheme.dimensions.paddingSemiLarge)
            )
        ) {
            item {
                AccountHeader(
                    gradient = gradient,
                    accountAddress = accountAddress,
                    state = state,
                    onShowHideBalanceToggle = onShowHideBalanceToggle,
                    onHistoryClick = onHistoryClick,
                    onTransferClick = onTransferClick,
                    onApplySecuritySettingsClick = onApplySecuritySettingsClick,
                    onLockerDepositClick = onLockerDepositClick
                )
            }

            assetsView(
                assetsViewData = if (!state.isPricesDisabled) {
                    assetsViewData
                } else {
                    assetsViewData?.copy(prices = null)
                },
                state = state.assetsViewState,
                isLoadingBalance = state.isAccountBalanceLoading,
                action = AssetsViewAction.Click(
                    onFungibleClick = onFungibleTokenClick,
                    onNonFungibleItemClick = onNonFungibleItemClick,
                    onLSUClick = onLSUUnitClicked,
                    onPoolUnitClick = onPoolUnitClick,
                    onNextNFtsPageRequest = onNextNFTsPageRequest,
                    onStakesRequest = onStakesRequest,
                    onClaimClick = onClaimClick,
                    onCollectionClick = onCollectionClick,
                    onTabClick = onTabClick
                ),
                onInfoClick = onInfoClick
            )
        }
    }
}

@Composable
private fun AccountHeader(
    gradient: Brush,
    accountAddress: AccountAddress?,
    state: State,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onApplySecuritySettingsClick: () -> Unit,
    onLockerDepositClick: (AccountLockerDeposit) -> Unit,
    modifier: Modifier = Modifier
) {
    Box {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(brush = gradient)
                .padding(horizontal = RadixTheme.dimensions.paddingLarge)
                .padding(bottom = RadixTheme.dimensions.paddingSemiLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            accountAddress?.let {
                ActionableAddressView(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                    address = Address.Account(it),
                    textStyle = RadixTheme.typography.body2HighImportance,
                    textColor = RadixTheme.colors.white.copy(alpha = 0.6f),
                    iconColor = RadixTheme.colors.white.copy(alpha = 0.6f)
                )
            }

            if (!state.isPricesDisabled) {
                TotalFiatBalanceView(
                    modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingXXLarge),
                    fiatPrice = state.totalFiatValue,
                    isLoading = state.isAccountBalanceLoading,
                    currency = SupportedCurrency.USD,
                    contentColor = RadixTheme.colors.white,
                    shimmeringColor = RadixTheme.colors.defaultBackground.copy(alpha = 0.6f),
                    formattedContentStyle = RadixTheme.typography.header,
                    onVisibilityToggle = onShowHideBalanceToggle,
                    trailingContent = {
                        TotalFiatBalanceViewToggle(onToggle = onShowHideBalanceToggle)
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                visible = state.isTransferEnabled,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RadixTheme.dimensions.paddingXXSmall),
                    horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                ) {
                    if (accountAddress != null) {
                        HistoryButton(
                            modifier = Modifier
                                .heightIn(min = 50.dp)
                                .weight(1f),
                            onHistoryClick = {
                                onHistoryClick(accountAddress)
                            }
                        )

                        TransferButton(
                            modifier = Modifier
                                .heightIn(min = 50.dp)
                                .weight(1f),
                            accountAddress = accountAddress,
                            onTransferClick = onTransferClick
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingDefault),
                visible = state.securityPrompts != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    state.securityPrompts?.forEach { securityPromptType ->
                        AccountPromptLabel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(bottom = RadixTheme.dimensions.paddingSmall),
                            onClick = {
                                onApplySecuritySettingsClick()
                            },
                            text = securityPromptType.toText()
                        )
                    }

                    state.deposits.forEach { deposit ->
                        AccountPromptLabel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(bottom = RadixTheme.dimensions.paddingSmall),
                            onClick = { onLockerDepositClick(deposit) },
                            text = stringResource(
                                id = R.string.homePage_accountLockerClaim,
                                deposit.dAppName.dAppDisplayName()
                            ),
                            iconRes = com.babylon.wallet.android.designsystem.R.drawable.ic_notifications
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(RadixTheme.dimensions.paddingSemiLarge)
                .background(
                    color = RadixTheme.colors.backgroundSecondary,
                    shape = RadixTheme.shapes.roundedRectTopDefault
                )
        )
    }
}

@Composable
private fun TransferButton(
    modifier: Modifier = Modifier,
    accountAddress: AccountAddress,
    onTransferClick: (AccountAddress) -> Unit
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
        }
    )
}

@UsesSampleValues
@Preview
@Composable
fun AccountContentPreview() {
    RadixWalletPreviewTheme {
        AccountScreenContent(
            state = State(
                pricesState = State.PricesState.Enabled(emptyMap()),
                accountWithAssets = AccountWithAssets(
                    account = Account.sampleMainnet(),
                    assets = Assets()
                ),
                securityPrompts = listOf(
                    SecurityPromptType.WRITE_DOWN_SEED_PHRASE,
                    SecurityPromptType.CONFIGURATION_BACKUP_PROBLEM
                )
            ),
            onShowHideBalanceToggle = {},
            onAccountPreferenceClick = { _ -> },
            onBackClick = {},
            onRefresh = {},
            onTransferClick = {},
            onMessageShown = {},
            onTabClick = {},
            onCollectionClick = {},
            onFungibleItemClicked = {},
            onNonFungibleItemClicked = { _, _ -> },
            onApplySecuritySettingsClick = {},
            onLockerDepositClick = {},
            onPoolUnitClick = {},
            onLSUUnitClicked = {},
            onNextNFTsPageRequest = {},
            onStakesRequest = {},
            onClaimClick = {},
            onHistoryClick = { _ -> },
            onInfoClick = {}
        )
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountContentWithFiatBalancesDisabledPreview() {
    RadixWalletPreviewTheme {
        AccountScreenContent(
            state = State(
                pricesState = State.PricesState.Disabled,
                accountWithAssets = AccountWithAssets(
                    account = Account.sampleMainnet()
                )
            ),
            onShowHideBalanceToggle = {},
            onAccountPreferenceClick = { _ -> },
            onBackClick = {},
            onRefresh = {},
            onTransferClick = {},
            onMessageShown = {},
            onTabClick = {},
            onCollectionClick = {},
            onFungibleItemClicked = {},
            onNonFungibleItemClicked = { _, _ -> },
            onApplySecuritySettingsClick = {},
            onLockerDepositClick = {},
            onPoolUnitClick = {},
            onLSUUnitClicked = {},
            onNextNFTsPageRequest = {},
            onStakesRequest = {},
            onClaimClick = {},
            onHistoryClick = { _ -> },
            onInfoClick = {}
        )
    }
}
