package com.babylon.wallet.android.presentation.dappdir.common.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.dappdir.common.models.DAppListItem.DAppWithDetails
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail
import com.babylon.wallet.android.presentation.ui.modifier.defaultCardShadow
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder
import com.babylon.wallet.android.presentation.ui.modifier.throttleClickable

@Composable
fun DAppCard(
    modifier: Modifier = Modifier,
    details: DAppWithDetails?,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .defaultCardShadow()
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(
                color = RadixTheme.colors.card,
                shape = RadixTheme.shapes.roundedRectMedium
            )
            .throttleClickable(onClick = onClick)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
        ) {
            Thumbnail.DApp(
                modifier = Modifier
                    .size(44.dp)
                    .radixPlaceholder(visible = details?.data == null),
                dAppIconUrl = details?.data?.iconUri,
                dAppName = details?.data?.name.orEmpty(),
                shape = RadixTheme.shapes.roundedRectSmall
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingXXSmall)
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction = if (details?.data == null) 0.5f else 1f
                        )
                        .radixPlaceholder(visible = details?.data == null),
                    text = details?.data?.name.orEmpty(),
                    style = RadixTheme.typography.secondaryHeader,
                    color = RadixTheme.colors.text,
                    maxLines = if (details?.data == null) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                val description = details?.data?.description
                if (details == null || details.isFetchingDAppDetails || !description.isNullOrBlank()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .radixPlaceholder(
                                visible = details == null || details.isFetchingDAppDetails
                            ),
                        text = description.orEmpty(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = RadixTheme.colors.text
                    )
                }
            }

            Icon(
                painter = painterResource(id = DSR.ic_chevron_right),
                contentDescription = null,
                tint = RadixTheme.colors.icon.copy(alpha = if (details != null) 1f else 0f),
            )
        }
    }
}
