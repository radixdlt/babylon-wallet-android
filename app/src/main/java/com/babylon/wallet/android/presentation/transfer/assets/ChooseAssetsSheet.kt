@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAssets
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.sheets.SheetHeader
import com.babylon.wallet.android.presentation.ui.composables.tabs.pagerTabIndicatorOffset
import kotlinx.coroutines.launch

@Composable
fun ChooseAssetsSheet(
    modifier: Modifier = Modifier,
    state: ChooseAssets,
    onTabSelected: (ChooseAssets.Tab) -> Unit,
    onCloseClick: () -> Unit,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit,
    onUiMessageShown: () -> Unit,
    onChooseAssetsSubmitted: () -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }

    SnackbarUIMessage(
        message = state.uiMessage,
        snackbarHostState = snackBarHostState,
        onMessageShown = onUiMessageShown
    )

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            SheetHeader(
                title = "Choose Asset(s)",
                onLeadingActionClicked = onCloseClick
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
            ) {
                Divider(color = RadixTheme.colors.gray5)

                RadixPrimaryButton(
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth(),
                    text = "Select Assets",
                    onClick = onChooseAssetsSubmitted,
                    enabled = state.targetAccount.assets.isNotEmpty()
                )
            }

        },
        snackbarHost = {
            RadixSnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(RadixTheme.dimensions.paddingLarge)
            )
        },
        containerColor = RadixTheme.colors.gray5
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pagerState = rememberPagerState()

            Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingDefault))

            ResourcesTabs(
                selectedTab = state.selectedTab, onTabSelected = onTabSelected,
                pagerState = pagerState
            )

            Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingDefault))

            Box(modifier = modifier.fillMaxSize()) {
                HorizontalPager(
                    pageCount = ChooseAssets.Tab.values().size,
                    state = pagerState,
                    userScrollEnabled = false
                ) { tabIndex ->
                    val tab = remember(tabIndex) {
                        ChooseAssets.Tab.values().find { it.ordinal == tabIndex } ?: ChooseAssets.Tab.Tokens
                    }

                    when (tab) {
                        ChooseAssets.Tab.Tokens -> FungibleAssetsChooser(
                            resources = state.resources?.fungibleResources.orEmpty(),
                            selectedAssets = state.targetAccount.assets,
                            onAssetSelectionChanged = onAssetSelectionChanged
                        )
                        ChooseAssets.Tab.NFTs -> NonFungibleAssetsChooser(
                            resources = state.resources?.nonFungibleResources.orEmpty(),
                            selectedAssets = state.targetAccount.assets,
                            onAssetSelectionChanged = onAssetSelectionChanged
                        )
                    }
                }

                if (state.resources == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RadixTheme.colors.gray1
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcesTabs(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    selectedTab: ChooseAssets.Tab,
    onTabSelected: (ChooseAssets.Tab) -> Unit
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedTab) {
        if (selectedTab.ordinal != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(page = selectedTab.ordinal) }
        }
    }

    TabRow(
        modifier = modifier.width(200.dp),
        selectedTabIndex = pagerState.currentPage,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { tabPositions ->
            Box(
                modifier = Modifier
                    .pagerTabIndicatorOffset(
                        pagerState = pagerState,
                        tabPositions = tabPositions,
                    )
                    .fillMaxHeight()
                    .zIndex(-1f)
                    .background(RadixTheme.colors.gray1, RadixTheme.shapes.circle)
            )
        }
    ) {
        ChooseAssets.Tab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(tab)
                    }
                },
                selectedContentColor = RadixTheme.colors.white,
                unselectedContentColor = RadixTheme.colors.gray1
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = RadixTheme.dimensions.paddingMedium,
                        vertical = RadixTheme.dimensions.paddingSmall
                    ),
                    text = tab.name(),
                    style = RadixTheme.typography.body1HighImportance,
                )
            }
        }
    }
}

@Composable
fun ChooseAssets.Tab.name(): String = when (this) {
    ChooseAssets.Tab.Tokens -> stringResource(id = R.string.account_asset_row_tab_tokens)
    ChooseAssets.Tab.NFTs -> stringResource(id = R.string.account_asset_row_tab_nfts)
}

@Preview
@Composable
private fun ChooseAssetsSheetPreview() {
    RadixWalletTheme {
        ChooseAssetsSheet(
            state = ChooseAssets(
                resources = Resources(
                    fungibleResources = listOf(),
                    nonFungibleResources = listOf()
                ),
                targetAccount = TargetAccount.Skeleton()
            ),
            onTabSelected = {},
            onCloseClick = {},
            onAssetSelectionChanged = { _, _ -> },
            onUiMessageShown = {},
            onChooseAssetsSubmitted = {}
        )
    }
}

