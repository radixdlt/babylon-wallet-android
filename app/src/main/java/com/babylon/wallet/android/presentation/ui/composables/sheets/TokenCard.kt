package com.babylon.wallet.android.presentation.ui.composables.sheets

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.Thumbnail

@Composable
fun FungibleCard(
    fungible: Resource.FungibleResource,
    modifier: Modifier = Modifier,
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
        Thumbnail.Fungible(
            modifier = Modifier.size(44.dp),
            token = fungible
        )
        Text(
            modifier = Modifier.weight(1f),
            text = fungible.displayTitle.ifEmpty { stringResource(id = R.string.authorizedDapps_dAppDetails_unknownTokenName) },
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
fun NonFungibleCard(
    nonFungible: Resource.NonFungibleResource,
    modifier: Modifier = Modifier,
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
        Thumbnail.NonFungible(
            modifier = Modifier.size(44.dp),
            collection = nonFungible,
            shape = Thumbnail.Shape.RoundedRectangle(8.dp)
        )
        Text(
            modifier = Modifier.weight(1f),
            text = nonFungible.name.ifEmpty { stringResource(id = R.string.authorizedDapps_dAppDetails_unknownTokenName) },
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
