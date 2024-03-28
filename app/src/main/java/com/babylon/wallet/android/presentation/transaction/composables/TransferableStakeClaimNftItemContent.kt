package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable
import rdx.works.core.displayableQuantity
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource

@Composable
fun TransferableStakeClaimNftItemContent(
    modifier: Modifier = Modifier,
    transferable: TransferableAsset.NonFungible.StakeClaimAssets,
    shape: Shape,
    onClick: (TransferableAsset.NonFungible.StakeClaimAssets, Resource.NonFungibleResource.Item) -> Unit
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
            verticalAlignment = CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(42.dp),
                collection = transferable.resource
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transferable.resource.name.ifEmpty { stringResource(id = R.string.transactionReview_unknown) },
                    style = RadixTheme.typography.body1Header,
                    color = RadixTheme.colors.gray1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = transferable.claim.validator.name,
                    style = RadixTheme.typography.body2Regular,
                    color = RadixTheme.colors.gray2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
//            Icon(painter = painterResource(id = DSR.ic_info_outline), contentDescription = null, tint = RadixTheme.colors.gray3)
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = RadixTheme.dimensions.paddingSmall),
            text = stringResource(id = R.string.transactionReview_toBeClaimed).uppercase(),
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray2,
            maxLines = 1
        )
        transferable.resource.items.forEachIndexed { index, item ->
            val addSpacer = index != transferable.resource.items.lastIndex
            Row(
                modifier = Modifier
                    .clip(RadixTheme.shapes.roundedRectSmall)
                    .throttleClickable {
                        onClick(
                            transferable,
                            item
                        )
                    }
                    .fillMaxWidth()
                    .border(1.dp, RadixTheme.colors.gray3, shape = RadixTheme.shapes.roundedRectSmall)
                    .padding(RadixTheme.dimensions.paddingMedium),
                verticalAlignment = CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
            ) {
                Icon(
                    painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_xrd_token),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RadixTheme.shapes.circle),
                    tint = Color.Unspecified
                )
                Text(
                    text = XrdResource.SYMBOL,
                    style = RadixTheme.typography.body2HighImportance,
                    color = RadixTheme.colors.gray1,
                    maxLines = 2
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = transferable.xrdWorthPerNftItem[item.localId.displayable]?.displayableQuantity().orEmpty(),
                    style = RadixTheme.typography.body1HighImportance,
                    color = RadixTheme.colors.gray1,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
            if (addSpacer) {
                Spacer(modifier = Modifier.height(RadixTheme.dimensions.paddingMedium))
            }
        }
    }
}
