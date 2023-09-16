package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.DAppWithMetadata

@Composable
fun DappCard(
    modifier: Modifier = Modifier,
    dApp: DAppWithMetadata,
    showChevron: Boolean = true,
    elevation: Dp = 8.dp
) {
    Row(
        modifier = modifier
            .shadow(elevation = elevation, shape = RadixTheme.shapes.roundedRectMedium)
            .clip(RadixTheme.shapes.roundedRectMedium)
            .fillMaxWidth()
            .background(RadixTheme.colors.white, shape = RadixTheme.shapes.roundedRectMedium)
            .padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingDefault
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        Thumbnail.DApp(
            modifier = Modifier.size(44.dp),
            dapp = dApp,
            shape = Thumbnail.Shape.RoundedRectangle(RadixTheme.dimensions.paddingSmall)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = dApp.displayName(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            Icon(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}

@Composable
fun StandardOneLineCard(
    image: String,
    title: String,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    placeholder: Painter = painterResource(id = R.drawable.img_placeholder)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        AsyncImage(
            model = image,
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RadixTheme.shapes.circle)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showChevron) {
            Icon(
                painter = painterResource(
                    id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right
                ),
                contentDescription = null,
                tint = RadixTheme.colors.gray1
            )
        }
    }
}
