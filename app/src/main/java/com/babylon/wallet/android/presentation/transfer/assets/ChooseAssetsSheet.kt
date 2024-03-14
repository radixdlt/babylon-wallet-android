package com.babylon.wallet.android.presentation.transfer.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.composable.RadixPrimaryButton
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.transfer.TargetAccount
import com.babylon.wallet.android.presentation.transfer.TransferViewModel.State.Sheet.ChooseAssets
import com.babylon.wallet.android.presentation.ui.composables.BottomDialogHeader
import com.babylon.wallet.android.presentation.ui.composables.RadixSnackbarHost
import com.babylon.wallet.android.presentation.ui.composables.SnackbarUIMessage
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewAction
import com.babylon.wallet.android.presentation.ui.composables.assets.AssetsViewData
import com.babylon.wallet.android.presentation.ui.composables.assets.assetsView

@Suppress("CyclomaticComplexMethod")
@Composable
fun ChooseAssetsSheet(
    modifier: Modifier = Modifier,
    state: ChooseAssets,
    onTabClick: (AssetsTab) -> Unit,
    onCollectionClick: (String) -> Unit,
    onCloseClick: () -> Unit,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit,
    onNextNFtsPageRequest: (Resource.NonFungibleResource) -> Unit,
    onStakesRequest: () -> Unit,
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
            BottomDialogHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = RadixTheme.colors.defaultBackground,
                    ),
                title = stringResource(id = R.string.assetTransfer_addAssets_navigationTitle),
                onDismissRequest = onCloseClick
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(color = RadixTheme.colors.defaultBackground)
            ) {
                HorizontalDivider(color = RadixTheme.colors.gray5)

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
        val selectedAssets = remember(state.targetAccount.spendingAssets) {
            state.targetAccount.spendingAssets.map { it.address }
        }

        val assetsViewData = remember(state.assets, state.assetsWithAssetsPrices, state.epoch) {
            AssetsViewData.from(
                assets = state.assets,
                prices = state.assetsWithAssetsPrices,
                epoch = state.epoch
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingSemiLarge)
        ) {
            assetsView(
                assetsViewData = assetsViewData,
                isLoadingBalance = state.isAccountBalanceLoading,
                state = state.assetsViewState,
                action = AssetsViewAction.Selection(
                    selectedResources = selectedAssets,
                    onFungibleCheckChanged = { fungible, isChecked ->
                        onAssetSelectionChanged(SpendingAsset.Fungible(resource = fungible), isChecked)
                    },
                    onNFTCheckChanged = { collection, nft, isChecked ->
                        onAssetSelectionChanged(SpendingAsset.NFT(collection, nft), isChecked)
                    },
                    onNextNFtsPageRequest = onNextNFtsPageRequest,
                    onStakesRequest = onStakesRequest,
                    onTabClick = onTabClick,
                    onCollectionClick = onCollectionClick,
                )
            )
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
            onTabClick = {},
            onCollectionClick = {},
            onCloseClick = {},
            onAssetSelectionChanged = { _, _ -> },
            onNextNFtsPageRequest = {},
            onStakesRequest = {},
            onUiMessageShown = {},
            onChooseAssetsSubmitted = {}
        )
    }
}
