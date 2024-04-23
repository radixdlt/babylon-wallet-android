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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.composable.RadixSecondaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.usecases.SecurityPromptType
import com.babylon.wallet.android.presentation.transfer.assets.AssetsTab
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.DefaultPullToRefreshContainer
import com.babylon.wallet.android.presentation.ui.composables.LocalDevBannerState
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.ThrottleIconButton
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewAction
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewData
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceView
import com.babylon.wallet.android.presentation.ui.composables.assets.TotalFiatBalanceViewToggle
import com.babylon.wallet.android.presentation.ui.composables.assets.assetsView
import com.babylon.wallet.android.presentation.ui.composables.toText
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.annotation.UsesSampleValues
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
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
    onNavigateToMnemonicBackup: (FactorSourceId.Hash) -> Unit,
    onNavigateToMnemonicRestore: () -> Unit,
    onFungibleResourceClick: (Resource.FungibleResource, Account) -> Unit,
    onNonFungibleResourceClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Account) -> Unit,
    onTransferClick: (AccountAddress) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.oneOffEvent.collect {
            when (it) {
                is AccountEvent.NavigateToMnemonicBackup -> onNavigateToMnemonicBackup(it.factorSourceId)
                is AccountEvent.NavigateToMnemonicRestore -> onNavigateToMnemonicRestore()
                is AccountEvent.OnFungibleClick -> onFungibleResourceClick(it.resource, it.account)
                is AccountEvent.OnNonFungibleClick -> onNonFungibleResourceClick(it.resource, it.item, it.account)
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
        onApplySecuritySettings = viewModel::onApplySecuritySettings,
        onPoolUnitClick = viewModel::onPoolUnitClicked,
        onLSUUnitClicked = viewModel::onLSUUnitClicked,
        onNextNFTsPageRequest = viewModel::onNextNftPageRequest,
        onStakesRequest = viewModel::onStakesRequest,
        onClaimClick = viewModel::onClaimClick,
        onTabClick = viewModel::onTabSelected,
        onCollectionClick = viewModel::onCollectionToggle,
        onHistoryClick = onHistoryClick
    )
}

@Composable
private fun AccountScreenContent(
    modifier: Modifier = Modifier,
    state: AccountUiState,
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
    onApplySecuritySettings: (SecurityPromptType) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
    onClaimClick: (List<StakeClaim>) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit
) {
    val gradient = remember(state.accountWithAssets) {
        getAccountGradientColorsFor(state.accountWithAssets?.account?.appearanceId?.value ?: 0u)
    }.toPersistentList()

    val snackBarHostState = remember { SnackbarHostState() }
    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onMessageShown
    )

    val lazyListState = rememberLazyListState()
    DefaultPullToRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.background(Brush.horizontalGradient(gradient))
    ) {
        Scaffold(
            modifier = Modifier,
            topBar = {
                RadixCenteredTopAppBar(
                    title = state.accountWithAssets?.account?.displayName?.value.orEmpty(),
                    onBackClick = onBackClick,
                    contentColor = RadixTheme.colors.white,
                    containerColor = Color.Transparent,
                    actions = {
                        // TODO revisit after compose update and remove if library update fixes the issue
                        // https://radixdlt.atlassian.net/browse/ABW-2504
                        ThrottleIconButton(
                            onClick = {
                                state.accountWithAssets?.account?.let {
                                    onAccountPreferenceClick(it.address)
                                }
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
                onApplySecuritySettings = onApplySecuritySettings,
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
            )
        }
    }
}

@Composable
fun AssetsContent(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    state: AccountUiState,
    onShowHideBalanceToggle: (isVisible: Boolean) -> Unit,
    onTabClick: (AssetsTab) -> Unit,
    onCollectionClick: (String) -> Unit,
    onFungibleTokenClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleItemClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit,
    onPoolUnitClick: (PoolUnit) -> Unit,
    gradient: ImmutableList<Color>,
    onTransferClick: (AccountAddress) -> Unit,
    onHistoryClick: (AccountAddress) -> Unit,
    onApplySecuritySettings: (SecurityPromptType) -> Unit,
    onLSUUnitClicked: (LiquidStakeUnit) -> Unit,
    onNextNFTsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
    onClaimClick: (List<StakeClaim>) -> Unit
) {
    Surface(
        modifier = modifier,
        color = RadixTheme.colors.gray5
    ) {
        val accountAddress = remember(state.accountWithAssets) {
            state.accountWithAssets?.account?.address
        }

        val assetsViewData = remember(state.accountWithAssets?.assets, state.assetsWithAssetsPrices, state.epoch) {
            AssetsViewData.from(
                assets = state.accountWithAssets?.assets,
                prices = state.assetsWithAssetsPrices,
                epoch = state.epoch
            )
        }

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
                            .padding(horizontal = RadixTheme.dimensions.paddingXXLarge)
                            .padding(bottom = RadixTheme.dimensions.paddingSemiLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        accountAddress?.let {
                            ActionableAddressView(
                                modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingSmall),
                                address = Address.Account(it),
                                textStyle = RadixTheme.typography.body2HighImportance,
                                textColor = RadixTheme.colors.white
                            )
                        }

                        if (state.isFiatBalancesEnabled) {
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
                                    .padding(horizontal = RadixTheme.dimensions.paddingXSmall),
                                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
                            ) {
                                if (accountAddress != null) {
                                    HistoryButton(
                                        modifier = Modifier.weight(1f),
                                        onHistoryClick = {
                                            onHistoryClick(accountAddress)
                                        }
                                    )

                                    TransferButton(
                                        modifier = Modifier.weight(1f),
                                        accountAddress = accountAddress,
                                        onTransferClick = onTransferClick
                                    )
                                }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            modifier = Modifier.padding(bottom = RadixTheme.dimensions.paddingLarge),
                            visible = state.securityPromptType != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ApplySecuritySettingsLabel(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    state.securityPromptType?.let(onApplySecuritySettings)
                                },
                                text = state.securityPromptType?.toText().orEmpty()
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
                assetsViewData = if (state.isFiatBalancesEnabled) {
                    assetsViewData
                } else {
                    assetsViewData?.copy(prices = null)
                },
                state = state.assetsViewState,
                isLoadingBalance = if (state.isFiatBalancesEnabled) {
                    state.isAccountBalanceLoading
                } else {
                    false
                },
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
                )
            )
        }
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
        with(SampleDataProvider()) {
            AccountScreenContent(
                state = AccountUiState(
                    accountWithAssets = sampleAccountWithoutResources(),
                    assetsWithAssetsPrices = emptyMap()
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
                onApplySecuritySettings = {},
                onPoolUnitClick = {},
                onLSUUnitClicked = {},
                onNextNFTsPageRequest = {},
                onStakesRequest = {},
                onClaimClick = {}
            ) {}
        }
    }
}

@UsesSampleValues
@Preview
@Composable
fun AccountContentWithFiatBalancesDisabledPreview() {
    RadixWalletPreviewTheme {
        with(SampleDataProvider()) {
            AccountScreenContent(
                state = AccountUiState(
                    isFiatBalancesEnabled = false,
                    accountWithAssets = sampleAccountWithoutResources(),
                    assetsWithAssetsPrices = emptyMap()
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
                onApplySecuritySettings = {},
                onPoolUnitClick = {},
                onLSUUnitClicked = {},
                onNextNFTsPageRequest = {},
                onStakesRequest = {},
                onClaimClick = {}
            ) {}
        }
    }
}
