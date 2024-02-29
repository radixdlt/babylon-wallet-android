package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import rdx.works.core.domain.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Other
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Owned
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    onTransferableFungibleClick: (asset: TransferableAsset.Fungible) -> Unit,
    onTransferableNonFungibleClick: (asset: TransferableAsset.NonFungible, Resource.NonFungibleResource.Item) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransactionAccountCardHeader(
            account = account,
            shape = RadixTheme.shapes.roundedRectTopMedium
        )

        account.resources.forEachIndexed { index, transferable ->
            val lastAsset = index == account.resources.lastIndex
            val shape = if (lastAsset) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape

            when (val asset = transferable.transferable) {
                is TransferableAsset.Fungible.Token -> TransferableTokenItemContent(
                    modifier = Modifier.throttleClickable {
                        onTransferableFungibleClick(asset)
                    },
                    transferable = transferable,
                    shape = shape,
                )

                is TransferableAsset.NonFungible.NFTAssets -> {
                    // Show each nft item
                    asset.resource.items.forEachIndexed { itemIndex, item ->
                        val lastNFT = itemIndex == asset.resource.items.lastIndex
                        TransferableNftItemContent(
                            modifier = Modifier.throttleClickable {
                                onTransferableNonFungibleClick(asset, item)
                            },
                            resource = asset.resource,
                            shape = if (lastAsset && lastNFT) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                            nftItem = item
                        )
                    }
                }

                is TransferableAsset.Fungible.PoolUnitAsset -> TransferablePoolUnitItemContent(
                    transferable = transferable,
                    shape = shape,
                    onClick = onTransferableFungibleClick
                )

                is TransferableAsset.Fungible.LSUAsset -> TransferableLsuItemContent(
                    modifier = Modifier.throttleClickable {
                        onTransferableFungibleClick(asset)
                    },
                    transferable = transferable,
                    shape = shape,
                )

                is TransferableAsset.NonFungible.StakeClaimAssets -> TransferableStakeClaimNftItemContent(
                    transferable = asset,
                    shape = shape,
                    onClick = onTransferableNonFungibleClick
                )
            }

            if (lastAsset.not()) {
                HorizontalDivider(color = RadixTheme.colors.gray4)
            }
        }
    }
}

@Composable
fun TransactionAccountCardHeader(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    shape: Shape = RadixTheme.shapes.roundedRectTopMedium
) {
    AccountCardHeader(
        displayName = when (account) {
            is Other -> stringResource(id = R.string.transactionReview_externalAccountName)
            is Owned -> account.account.displayName
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = when (account) {
                    is Other -> SolidColor(RadixTheme.colors.gray2)
                    is Owned -> Brush.linearGradient(getAccountGradientColorsFor(account.account.appearanceID))
                },
                shape = shape
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
fun AccountDepositAccountCardHeader(account: Network.Account, modifier: Modifier = Modifier) {
    AccountCardHeader(
        displayName = account.displayName,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(getAccountGradientColorsFor(account.appearanceID)),
                shape = RadixTheme.shapes.roundedRectTopMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
private fun AccountCardHeader(modifier: Modifier = Modifier, displayName: String, address: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = displayName,
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            modifier = Modifier.weight(1f, false),
            color = RadixTheme.colors.white
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        ActionableAddressView(
            address = address,
            textStyle = RadixTheme.typography.body2Regular,
            textColor = RadixTheme.colors.white,
            iconColor = RadixTheme.colors.white
        )
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardPreview() {
    RadixWalletTheme {
        TransactionAccountCard(
            account = Owned(
                account = SampleDataProvider().sampleAccount(),
                resources = SampleDataProvider().sampleFungibleResources().map {
                    Transferable.Withdrawing(
                        transferable = TransferableAsset.Fungible.Token(
                            amount = "689.203".toBigDecimal(),
                            resource = it,
                            isNewlyCreated = false
                        )
                    )
                }
            ),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleClick = { _, _ -> }
        )
    }
}
