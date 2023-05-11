package com.babylon.wallet.android.presentation.account

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.domain.model.AccountWithResources
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import rdx.works.core.displayableQuantity
import java.math.BigDecimal

@Composable
fun FungibleItemRow(
    fungible: AccountWithResources.Resource.FungibleResource,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = RadixTheme.dimensions.paddingLarge,
                vertical = RadixTheme.dimensions.paddingMedium
            ),
            horizontalArrangement = Arrangement.spacedBy(RadixTheme.dimensions.paddingMedium)
        ) {
            val placeholder = if (fungible.isXrd) {
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
                    model = rememberImageUrl(fromUrl = fungible.iconUrl.toString(), size = ImageSize.MEDIUM),
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
                text = fungible.displayTitle,
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = fungible.amount.displayableQuantity(),
                style = RadixTheme.typography.body2HighImportance,
                color = RadixTheme.colors.gray1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview("default")
@Preview("large font", fontScale = 2f)
@Composable
fun TokenItemCardPreview() {
    RadixWalletTheme {
        FungibleItemRow(
            fungible = AccountWithResources.Resource.FungibleResource(
                resourceAddress = "account_rdx_abc",
                amount = BigDecimal(1234.5678),
                nameMetadataItem = NameMetadataItem("a very long name that might cause troubles"),
                symbolMetadataItem = SymbolMetadataItem("BTC"),
                iconUrlMetadataItem = IconUrlMetadataItem("https://some.icon".toUri()),
                descriptionMetadataItem = DescriptionMetadataItem("Bitcoin")
            )
        )
    }
}

@Preview("default with long name and long values")
@Preview("large font with long name and long values", fontScale = 2f)
@Composable
fun TokenItemCardWithLongNameAndLongValuesPreview() {
    RadixWalletTheme {
        FungibleItemRow(
            fungible = AccountWithResources.Resource.FungibleResource(
                resourceAddress = "account_rdx_abc",
                amount = BigDecimal(1234567.890123),
                nameMetadataItem = NameMetadataItem("Radix"),
                symbolMetadataItem = SymbolMetadataItem("XRD")
            )
        )
    }
}
