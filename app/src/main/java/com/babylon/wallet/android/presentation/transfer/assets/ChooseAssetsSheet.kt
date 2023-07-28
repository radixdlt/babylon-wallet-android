@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAssets
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.resources.FungibleResourcesColumn
import com.babylon.wallet.android.presentation.ui.composables.resources.NonFungibleResourcesColumn
import com.babylon.wallet.android.presentation.ui.composables.resources.SelectableFungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.resources.SelectableNonFungibleResourceItem
import com.babylon.wallet.android.presentation.ui.composables.sheets.SheetHeader

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
                title = stringResource(id = R.string.assetTransfer_addAssets_navigationTitle),
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
                    text = when (val count = state.assetsSelectedCount) {
                        0 -> stringResource(id = R.string.assetTransfer_addAssets_buttonAssetsNone)
                        1 -> stringResource(id = R.string.assetTransfer_addAssets_buttonAssetsOne)
                        else -> stringResource(id = R.string.assetTransfer_addAssets_buttonAssets, count)
                    },
                    onClick = onChooseAssetsSubmitted,
                    modifier = Modifier
                        .padding(RadixTheme.dimensions.paddingDefault)
                        .fillMaxWidth(),
                    enabled = state.isSubmitEnabled
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pagerState = rememberPagerState()

            Spacer(modifier = Modifier.height(height = RadixTheme.dimensions.paddingLarge))

            ResourcesTabs(
                selectedTab = when (state.selectedTab) {
                    ChooseAssets.Tab.Tokens -> ResourceTab.Tokens
                    ChooseAssets.Tab.NFTs -> ResourceTab.Nfts
                },
                onTabSelected = {
                    val viewModelTab = when (it) {
                        ResourceTab.Tokens -> ChooseAssets.Tab.Tokens
                        ResourceTab.Nfts -> ChooseAssets.Tab.NFTs
                    }
                    onTabSelected(viewModelTab)
                },
                pagerState = pagerState
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.resources != null) {
                    HorizontalPager(
                        pageCount = ChooseAssets.Tab.values().size,
                        state = pagerState,
                        userScrollEnabled = false
                    ) { tabIndex ->
                        val tab = remember(tabIndex) {
                            ChooseAssets.Tab.values().find { it.ordinal == tabIndex } ?: ChooseAssets.Tab.Tokens
                        }

                        when (tab) {
                            ChooseAssets.Tab.Tokens -> FungibleResourcesColumn(
                                modifier = Modifier.fillMaxSize(),
                                resources = state.resources
                            ) { _, resource ->
                                val isSelected = state.targetAccount.assets.any { it.address == resource.resourceAddress }
                                SelectableFungibleResourceItem(
                                    modifier = Modifier
                                        .heightIn(min = 85.dp)
                                        .clickable {
                                            val fungibleAsset = SpendingAsset.Fungible(resource = resource)
                                            onAssetSelectionChanged(fungibleAsset, !isSelected)
                                        },
                                    resource = resource,
                                    isSelected = isSelected,
                                    onCheckChanged = {
                                        val fungibleAsset = SpendingAsset.Fungible(resource = resource)
                                        onAssetSelectionChanged(fungibleAsset, it)
                                    }
                                )
                            }

                            ChooseAssets.Tab.NFTs -> NonFungibleResourcesColumn(
                                resources = state.resources,
                                modifier = Modifier.fillMaxSize(),
                            ) { _, item ->
                                val isSelected = state.targetAccount.assets.any { it.address == item.globalAddress }
                                SelectableNonFungibleResourceItem(
                                    modifier = Modifier.clickable {
                                        val nonFungibleAsset = SpendingAsset.NFT(item = item)
                                        onAssetSelectionChanged(nonFungibleAsset, !isSelected)
                                    },
                                    item = item,
                                    isSelected = isSelected,
                                    onCheckChanged = {
                                        val nonFungibleAsset = SpendingAsset.NFT(item = item)
                                        onAssetSelectionChanged(nonFungibleAsset, it)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RadixTheme.colors.gray1
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChooseAssetsSheetPreview() {
    RadixWalletTheme {
        ChooseAssetsSheet(
            state = ChooseAssets.init(
                forTargetAccount = TargetAccount.Skeleton()
            ),
            onTabSelected = {},
            onCloseClick = {},
            onAssetSelectionChanged = { _, _ -> },
            onUiMessageShown = {},
            onChooseAssetsSubmitted = {}
        )
    }
}
