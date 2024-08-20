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
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.model.displayTitle
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail

@Composable
fun TransferableTokenItemContent(
    modifier: Modifier = Modifier,
    transferable: Transferable,
    shape: Shape,
    isHidden: Boolean
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
            when (val resource = transferable.transferable) {
                is TransferableAsset.Fungible.Token -> {
                    Thumbnail.Fungible(
                        modifier = Modifier.size(44.dp),
                        token = resource.resource,
                    )
                }

                is TransferableAsset.NonFungible.NFTAssets -> {
                    Thumbnail.NonFungible(
                        modifier = Modifier.size(44.dp),
                        collection = resource.resource
                    )
                }

                else -> {}
            }
            Text(
                modifier = Modifier.weight(1f),
                text = transferable.transferable.displayTitle(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            GuaranteesSection(transferable)
        }
        TransferableHiddenItemWarning(isHidden = isHidden)
    }
}
