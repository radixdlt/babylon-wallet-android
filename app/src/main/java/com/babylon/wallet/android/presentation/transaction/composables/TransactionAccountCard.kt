package com.babylon.wallet.android.presentation.transaction.composables

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Other
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources.Owned
import com.babylon.wallet.android.presentation.ui.composables.ActionableAddressView
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import rdx.works.core.displayableQuantity

@Composable
fun TransactionAccountCard(
    modifier: Modifier = Modifier,
    account: AccountWithTransferableResources
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = when (account) {
                        is Other -> SolidColor(RadixTheme.colors.gray2)
                        is Owned -> Brush.linearGradient(getAccountGradientColorsFor(account.account.appearanceID))
                    },
                    shape = RadixTheme.shapes.roundedRectTopMedium
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

            TokenItemContent(
                isXrdToken = transferableAmount.resource.isXrd,
                tokenUrl = transferableAmount.resource.iconUrl.toString(),
                tokenSymbol = transferableAmount.resource.displayTitle.ifEmpty {
                    stringResource(id = com.babylon.wallet.android.R.string.transactionReview_unknown)
                },
                tokenAmount = transferableAmount.amount.toPlainString(),
                isTokenAmountVisible = true,
                guaranteedQuantity = amountGuaranteeAssertion?.amount?.displayableQuantity(),
                shape = shape
            )
        }


        // Non fungibles
        nftTransferables.forEachIndexed { collectionIndex, nftTransferable ->
            val nft = nftTransferable.transferable as TransferableResource.NFTs
            if (nft.isNewlyCreated) {
                // In this case show only the collection of the newly created nfts.
                val lastItem = collectionIndex == nftTransferables.lastIndex
                TokenItemContent(
                    isXrdToken = false,
                    tokenUrl = nft.resource.iconUrl.toString(),
                    tokenSymbol = nft.resource.name,
                    isTokenAmountVisible = true,
                    tokenAmount = nft.resource.items.size.toString(),
                    shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                )
            } else {
                // Show each nft item
                nft.resource.items.forEachIndexed { itemIndex, item ->
                    val lastItem = itemIndex == nft.resource.items.lastIndex && collectionIndex ==  nftTransferables.lastIndex
                    TokenItemContent(
                        isXrdToken = false,
                        tokenUrl = item.imageUrl.toString(),
                        tokenSymbol = item.localId.displayable,
                        isTokenAmountVisible = false,
                        shape = if (lastItem) RadixTheme.shapes.roundedRectBottomMedium else RectangleShape
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenItemContent(
    isXrdToken: Boolean,
    tokenUrl: String,
    tokenSymbol: String?,
    tokenAmount: String? = null,
    isTokenAmountVisible: Boolean,
    guaranteedQuantity: String? = null,
    shape: Shape
) {
    Row(
        verticalAlignment = CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray4,
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
            rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
        ) {
            AsyncImage(
                model = rememberImageUrl(fromUrl = tokenUrl, size = ImageSize.SMALL),
                placeholder = placeholder,
                fallback = placeholder,
                error = placeholder,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RadixTheme.shapes.circle)
            )
        }
        Text(
            modifier = Modifier
                .weight(1f),
            text = tokenSymbol ?: stringResource(id = com.babylon.wallet.android.R.string.transactionReview_unknown),
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
                    tokenAmount?.let { amount ->
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
            )
        )
    }
}
