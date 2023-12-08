package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Other
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Owned
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import rdx.works.core.displayableQuantity
import rdx.works.profile.data.model.pernetwork.Network

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TransactionAccountCardHeader(
            account = account,
            shape = RadixTheme.shapes.roundedRectTopMedium
        )

        val amountTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableResource.Amount }
        }

        val nftTransferables = remember(account.resources) {
            account.resources.filter { it.transferable is TransferableResource.NFTs }
        }

        // Fungibles
        amountTransferables.forEachIndexed { index, amountTransferable ->
            val lastItem = if (nftTransferables.isEmpty()) index == amountTransferables.lastIndex else false
            val shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
            val transferableAmount = amountTransferable.transferable as TransferableResource.Amount

            TransferableItemContent(
                modifier = Modifier.clickable {
                    onFungibleResourceClick(transferableAmount.resource, transferableAmount.isNewlyCreated)
                },
                transferable = amountTransferable,
                shape = shape,
            )
        }

        // Non fungibles
        nftTransferables.forEachIndexed { collectionIndex, nftTransferable ->
            val collection = nftTransferable.transferable as TransferableResource.NFTs
            // Show each nft item
            collection.resource.items.forEachIndexed { itemIndex, item ->
                val lastItem = itemIndex == collection.resource.items.lastIndex && collectionIndex == nftTransferables.lastIndex
                TransferableNftItemContent(
                    modifier = Modifier.clickable { onNonFungibleResourceClick(collection.resource, item, collection.isNewlyCreated) },
                    transferable = collection,
                    shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                    nftItem = item
                )
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

@Composable
private fun TransferableItemContent(
    modifier: Modifier = Modifier,
    transferable: Transferable,
    shape: Shape
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        when (val resource = transferable.transferable) {
            is TransferableResource.Amount -> {
                Thumbnail.Fungible(
                    modifier = Modifier.size(44.dp),
                    token = resource.resource,
                )
            }

            is TransferableResource.NFTs -> {
                Thumbnail.NonFungible(
                    modifier = Modifier.size(44.dp),
                    collection = resource.resource,
                    shape = RadixTheme.shapes.roundedRectSmall
                )
            }
        }
        Text(
            modifier = Modifier.weight(1f),
            text = when (val resource = transferable.transferable) {
                is TransferableResource.Amount -> resource.resource.displayTitle
                is TransferableResource.NFTs -> resource.resource.name
            }.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            val guaranteedQuantity = transferable.guaranteeAssertion as? GuaranteeAssertion.ForAmount
            Row(
                modifier = Modifier,
                verticalAlignment = CenterVertically
            ) {
                if (guaranteedQuantity != null) {
                    Text(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = R.string.transactionReview_estimated),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }

                (transferable.transferable as? TransferableResource.Amount)?.let {
                    Text(
                        modifier = Modifier,
                        text = it.amount.displayableQuantity(),
                        style = RadixTheme.typography.secondaryHeader,
                        color = RadixTheme.colors.gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
            guaranteedQuantity?.let { quantity ->
                Row {
                    Text(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_guaranteed),
                        style = RadixTheme.typography.body2Regular,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text(
                        modifier = Modifier,
                        text = quantity.amount.displayableQuantity(),
                        style = RadixTheme.typography.body2HighImportance,
                        color = RadixTheme.colors.gray2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferableNftItemContent(
    modifier: Modifier = Modifier,
    transferable: TransferableResource.NFTs,
    shape: Shape,
    nftItem: Resource.NonFungibleResource.Item
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
    ) {
        Thumbnail.NonFungible(
            modifier = Modifier.size(44.dp),
            collection = transferable.resource,
            shape = RadixTheme.shapes.roundedRectSmall
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = nftItem.localId.displayable,
                style = RadixTheme.typography.body2Regular,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = (nftItem.name ?: transferable.resource.name).ifEmpty {
                    stringResource(id = R.string.transactionReview_unknown)
                },
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
                        transferable = TransferableResource.Amount(
                            amount = "689.203".toBigDecimal(),
                            resource = it,
                            isNewlyCreated = false
                        )
                    )
                }
            ),
            onFungibleResourceClick = { _, _ -> },
            onNonFungibleResourceClick = { _, _, _ -> }
        )
    }
}
