package com.babylon.wallet.android.presentation.transaction.composables

import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.designsystem.theme.getAccountGradientColorsFor
import com.babylon.wallet.android.domain.SampleDataProvider
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Other
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Owned
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import rdx.works.core.displayableQuantity

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources,
    onFungibleResourceClick: (fungibleResource: Resource.FungibleResource) -> Unit,
    onNonFungibleResourceClick: (nonFungibleResource: Resource.NonFungibleResource, Resource.NonFungibleResource.Item) -> Unit
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
            val amountGuaranteeAssertion = amountTransferable.guaranteeAssertion as? GuaranteeAssertion.ForAmount

            ResourceItemContent(
                isXrdToken = transferableAmount.resource.isXrd,
                iconUrl = transferableAmount.resource.iconUrl,
                symbol = transferableAmount.resource.displayTitle.ifEmpty {
                    stringResource(id = com.babylon.wallet.android.R.string.transactionReview_unknown)
                },
                amount = transferableAmount.amount.displayableQuantity(),
                isTokenAmountVisible = true,
                guaranteedQuantity = amountGuaranteeAssertion?.amount?.displayableQuantity(),
                shape = shape,
                isNft = false,
                onResourceClick = { onFungibleResourceClick(amountTransferable.transferable.resource as Resource.FungibleResource) }
            )
        }

        // Non fungibles
        nftTransferables.forEachIndexed { collectionIndex, nftTransferable ->
            val collection = nftTransferable.transferable as TransferableResource.NFTs
            // Show each nft item
            collection.resource.items.forEachIndexed { itemIndex, item ->
                val lastItem = itemIndex == collection.resource.items.lastIndex && collectionIndex == nftTransferables.lastIndex
                ResourceItemContent(
                    isXrdToken = false,
                    iconUrl = collection.resource.iconUrl,
                    symbol = item.localId.displayable,
                    isTokenAmountVisible = false,
                    shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape,
                    isNft = true,
                    onResourceClick = { onNonFungibleResourceClick(collection.resource, item) }
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
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
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
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = when (account) {
                is Other -> stringResource(id = com.babylon.wallet.android.R.string.transactionReview_externalAccountName)
                is Owned -> account.account.displayName
            },
            style = RadixTheme.typography.body1Header,
            maxLines = 1,
            modifier = Modifier.weight(1f, false),
            color = RadixTheme.colors.white
        )
        Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingSmall))
        ActionableAddressView(
            address = account.address,
            textStyle = RadixTheme.typography.body2Regular,
            textColor = RadixTheme.colors.white,
            iconColor = RadixTheme.colors.white
        )
    }
}

@Composable
private fun ResourceItemContent(
    isXrdToken: Boolean,
    iconUrl: Uri?,
    symbol: String?,
    amount: String? = null,
    isTokenAmountVisible: Boolean,
    guaranteedQuantity: String? = null,
    shape: Shape,
    isNft: Boolean,
    onResourceClick: () -> Unit
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier
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
        val placeholder = if (isXrdToken) {
            painterResource(id = R.drawable.ic_xrd_token)
        } else {
            if (isNft) {
                painterResource(id = R.drawable.ic_nfts)
            } else {
                painterResource(id = R.drawable.ic_token)
            }
        }
        var contentScale by remember {
            mutableStateOf(ContentScale.Crop)
        }
        AsyncImage(
            modifier = Modifier
                .size(44.dp)
                .background(RadixTheme.colors.gray4, shape = RadixTheme.shapes.circle)
                .clip(RadixTheme.shapes.circle)
                .clickable(role = Role.Button) { onResourceClick() },
            model = rememberImageUrl(fromUrl = iconUrl, size = ImageSize.SMALL),
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = contentScale,
            onError = {
                contentScale = ContentScale.Inside
            }
        )
        Text(
            modifier = Modifier
                .weight(1f),
            text = symbol ?: stringResource(id = com.babylon.wallet.android.R.string.transactionReview_unknown),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier,
                verticalAlignment = CenterVertically
            ) {
                if (guaranteedQuantity != null) {
                    Text(
                        modifier = Modifier.padding(end = RadixTheme.dimensions.paddingSmall),
                        text = stringResource(id = com.babylon.wallet.android.R.string.transactionReview_estimated),
                        style = RadixTheme.typography.body2Link,
                        color = RadixTheme.colors.gray1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
                if (isTokenAmountVisible) {
                    amount?.let { amount ->
                        Text(
                            modifier = Modifier,
                            text = amount,
                            style = RadixTheme.typography.secondaryHeader,
                            color = RadixTheme.colors.gray1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
            guaranteedQuantity?.let { guaranteedQuantity ->
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
                        text = guaranteedQuantity,
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
            onFungibleResourceClick = { _ -> },
            onNonFungibleResourceClick = { _, _ -> }
        )
    }
}
