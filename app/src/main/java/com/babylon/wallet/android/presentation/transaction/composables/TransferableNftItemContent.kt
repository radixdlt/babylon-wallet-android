package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.radixdlt.sargon.annotation.UsesSampleValues
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.sampleMainnet

@Composable
fun TransferableNftItemContent(
    modifier: Modifier = Modifier,
    shape: Shape,
    transferableNFTCollection: Transferable.NonFungibleType.NFTCollection,
    nftItem: Resource.NonFungibleResource.Item,
    isHidden: Boolean,
    hiddenResourceWarning: String
) {
    Column(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(
                color = RadixTheme.colors.gray5,
                shape = shape
            )
            .padding(
                horizontal = RadixTheme.dimensions.paddingDefault,
                vertical = RadixTheme.dimensions.paddingMedium
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = CenterVertically
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = transferableNFTCollection.asset.resource
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = transferableNFTCollection.asset.displayTitle(),
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = nftItem.displaySubtitle(),
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

            Spacer(modifier = Modifier.weight(1f))

            transferableNFTCollection.amount.additional?.let {
                CountedAmountSection(countedAmount = it)
            }
        }

        UnknownAmount(
            modifier = Modifier.padding(top = RadixTheme.dimensions.paddingSmall),
            amount = transferableNFTCollection.amount.additional
        )

        TransferableHiddenItemWarning(
            isHidden = isHidden,
            text = hiddenResourceWarning
        )
    }
}

@UsesSampleValues
@Preview(showBackground = true)
@Composable
private fun TransferableNftItemPreview(
    @PreviewParameter(CountedAmountSectionPreviewProvider::class) amount: CountedAmount
) {
    RadixWalletTheme {
        val asset = remember {
            NonFungibleCollection(collection = Resource.NonFungibleResource.sampleMainnet())
        }
        val nonFungibleAmount = remember(asset) {
            NonFungibleAmount(
                certain = asset.collection.items,
                additional = amount
            )
        }
        TransferableNftItemContent(
            transferableNFTCollection = Transferable.NonFungibleType.NFTCollection(
                asset = asset,
                amount = nonFungibleAmount,
                isNewlyCreated = false
            ),
            nftItem = Resource.NonFungibleResource.sampleMainnet().items.first(),
            shape = RectangleShape,
            isHidden = false,
            hiddenResourceWarning = stringResource(id = R.string.interactionReview_hiddenAsset_deposit),
        )
    }
}
