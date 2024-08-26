package com.babylon.wallet.android.presentation.transaction.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.model.displaySubtitle
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import rdx.works.core.domain.resources.Resource

@Composable
fun TransferableNftItemContent(
    modifier: Modifier = Modifier,
    shape: Shape,
    asset: TransferableAsset.NonFungible.NFTAssets,
    nftItem: Resource.NonFungibleResource.Item,
    isHidden: Boolean,
    hiddenWarning: String
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
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            Thumbnail.NonFungible(
                modifier = Modifier.size(44.dp),
                collection = asset.resource
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = asset.displayTitle(),
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
        }
        TransferableHiddenItemWarning(
            isHidden = isHidden,
            text = hiddenWarning
        )
    }
}
