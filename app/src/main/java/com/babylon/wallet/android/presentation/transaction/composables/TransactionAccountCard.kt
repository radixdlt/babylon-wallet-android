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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.gradient
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Address
import com.radixdlt.sargon.ResourceIdentifier
import com.radixdlt.sargon.annotation.UsesSampleValues
import com.radixdlt.sargon.extensions.toDecimal192
import com.radixdlt.sargon.samples.sampleMainnet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet
import rdx.works.core.sargon.fungibles
import rdx.works.core.sargon.nonFungibles
import rdx.works.core.sargon.pools

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    hiddenResourceIds: PersistentList<ResourceIdentifier>,
    hiddenResourceWarning: String,
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
                    isHidden = remember(
                        asset,
                        hiddenResourceIds
                    ) { asset.resource.address in hiddenResourceIds.fungibles() },
                    hiddenResourceWarning = hiddenResourceWarning
                )

                is TransferableAsset.NonFungible.NFTAssets -> {
                    // Show each nft item
                    asset.resource.items.forEachIndexed { itemIndex, item ->
                        val lastNFT = itemIndex == asset.resource.items.lastIndex
                        TransferableNftItemContent(
                            modifier = Modifier.throttleClickable {
                                onTransferableNonFungibleClick(asset, item)
                            },
                            asset = asset,
                            shape = if (lastAsset && lastNFT) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                            nftItem = item,
                            isHidden = remember(
                                item,
                                hiddenResourceIds
                            ) { item.collectionAddress in hiddenResourceIds.nonFungibles() },
                            hiddenResourceWarning = hiddenResourceWarning
                        )
                    }
                }

                is TransferableAsset.Fungible.PoolUnitAsset -> TransferablePoolUnitItemContent(
                    transferable = transferable,
                    shape = shape,
                    isHidden = remember(
                        asset,
                        hiddenResourceIds
                    ) { asset.resource.poolAddress in hiddenResourceIds.pools() },
                    hiddenResourceWarning = hiddenResourceWarning,
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
            is AccountWithTransferableResources.Other -> stringResource(id = R.string.transactionReview_externalAccountName)
            is AccountWithTransferableResources.Owned -> account.account.displayName.value
        },
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = when (account) {
                    is AccountWithTransferableResources.Other -> SolidColor(RadixTheme.colors.gray2)
                    is AccountWithTransferableResources.Owned -> account.account.appearanceId.gradient()
                },
                shape = shape
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
fun AccountDepositAccountCardHeader(account: Account, modifier: Modifier = Modifier) {
    AccountCardHeader(
        displayName = account.displayName.value,
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = account.appearanceId.gradient(),
                shape = RadixTheme.shapes.roundedRectTopMedium
            )
            .padding(RadixTheme.dimensions.paddingMedium),
        address = account.address
    )
}

@Composable
private fun AccountCardHeader(modifier: Modifier = Modifier, displayName: String, address: AccountAddress) {
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
            address = Address.Account(address),
            textStyle = RadixTheme.typography.body2Regular,
            textColor = RadixTheme.colors.white,
            iconColor = RadixTheme.colors.white
        )
    }
}

@UsesSampleValues
@Preview("default")
@Preview("large font", fontScale = 2f)
@Preview(showBackground = true)
@Composable
fun TransactionAccountCardPreview() {
    RadixWalletTheme {
        TransactionAccountCard(
            account = AccountWithTransferableResources.Owned(
                account = Account.sampleMainnet(),
                resources = listOf(
                    Transferable.Withdrawing(
                        transferable = TransferableAsset.Fungible.Token(
                            amount = 689.203.toDecimal192(),
                            resource = Resource.FungibleResource.sampleMainnet(),
                            isNewlyCreated = false
                        )
                    )
                )
            ),
            hiddenResourceIds = persistentListOf(),
            hiddenResourceWarning = stringResource(id = R.string.transactionReview_hiddenAsset_withdraw),
            onTransferableFungibleClick = { },
            onTransferableNonFungibleClick = { _, _ -> }
        )
    }
}
