@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)

package com.babylon.wallet.android.presentation.account

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
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
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab
import com.babylon.wallet.android.presentation.transfer.assets.ResourcesTabs
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ApplySecuritySettingsLabel
import com.babylon.wallet.android.presentation.ui.composables.RadixCenteredTopAppBar
import com.babylon.wallet.android.presentation.ui.composables.ScrollableHeaderView
import com.babylon.wallet.android.presentation.ui.composables.WalletBalanceView
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourcesColumn
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourcesColumn
import kotlinx.coroutines.CoroutineScope
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

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    val scope = rememberCoroutineScope()
//    val sheetHeight = maxHeight * 0.9f
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
            SheetContent(state, scope, bottomSheetState)
        },
    ) {
        val scrollConnection = rememberNestedScrollInteropConnection()
        Scaffold(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradient))
                .nestedScroll(scrollConnection),
            topBar = {
                RadixCenteredTopAppBar(
                    title = state.accountWithResources?.account?.displayName.orEmpty(),
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(
                            onClick = { onAccountPreferenceClick(state.accountWithResources?.account?.address.orEmpty()) }
                        ) {
                            Icon(
                                painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_more_horiz),
                                tint = RadixTheme.colors.white,
                                contentDescription = "account settings"
                            )
                        }
                    },
                    containerColor = Color.Transparent,
                    contentColor = RadixTheme.colors.white
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (state.isHistoryEnabled) {
                    HistoryButton(
                        modifier = Modifier.size(174.dp, 50.dp),
                        onHistoryClick
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->
            ScrollableHeaderView(
                modifier = modifier.padding(innerPadding),
                header = {
                    AccountCollapsibleContent(
                        state = state,
                        onTransferClick = onTransferClick,
                        onApplySecuritySettings = onApplySecuritySettings
                    )
                },
                content = {
                    AssetsContent(
                        modifier = Modifier
                            .background(RadixTheme.colors.gray5),
                        resources = state.accountWithResources?.resources,
                        onFungibleTokenClick = {
                            onFungibleResourceClicked(it)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        },
                        onNonFungibleItemClick = { nftCollection, nftItem ->
                            onNonFungibleItemClicked(nftCollection, nftItem)
                            scope.launch {
                                bottomSheetState.show()
                            }
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun SheetContent(
    state: AccountUiState,
    scope: CoroutineScope,
    bottomSheetState: ModalBottomSheetState
) {
    when (val selected = state.selectedResource) {
        is SelectedResource.SelectedNonFungibleResource -> {
            NonFungibleTokenBottomSheetDetails(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                item = selected.item,
                nonFungibleItem = state.selectedResource.nonFungible,
                onCloseClick = {
                    scope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
        }

        is SelectedResource.SelectedFungibleResource -> {
            FungibleTokenBottomSheetDetails(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                fungible = selected.fungible,
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
