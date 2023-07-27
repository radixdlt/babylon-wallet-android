package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.applyImageAspectRatio
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl

@Composable
fun NonFungibleResourceItem(
    modifier: Modifier = Modifier,
    item: Resource.NonFungibleResource.Item,
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(RadixTheme.dimensions.paddingDefault)
    ) {
        val brokenImagePlaceholder = painterResource(id = R.drawable.img_nft_broken_image)
        val placeholder = painterResource(id = R.drawable.img_placeholder)
        val imageUrl = item.imageUrl

        if (imageUrl != null) {
            val painter = rememberAsyncImagePainter(
                model = rememberImageUrl(
                    fromUrl = imageUrl,
                    size = ImageSize.LARGE
                ),
                placeholder = placeholder,
                error = brokenImagePlaceholder
            )
            Image(
                painter = painter,
                contentDescription = "Nft image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .applyImageAspectRatio(painter = painter)
                    .clip(RadixTheme.shapes.roundedRectMedium)
                    .background(Color.Transparent, RadixTheme.shapes.roundedRectMedium)
            )
        }

        item.nameMetadataItem?.name?.let { name ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = name,
                style = RadixTheme.typography.body1HighImportance,
                color = RadixTheme.colors.gray1
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = item.localId.displayable,
            style = RadixTheme.typography.body1HighImportance,
            color = RadixTheme.colors.gray2
        )
    }
}

@Composable
fun SelectableNonFungibleResourceItem(
    modifier: Modifier = Modifier,
    item: Resource.NonFungibleResource.Item,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .padding(vertical = RadixTheme.dimensions.paddingDefault)
            .padding(start = RadixTheme.dimensions.paddingDefault, end = RadixTheme.dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(RadixTheme.dimensions.paddingSmall)
    ) {
        NonFungibleResourceItem(
            modifier = Modifier.weight(1f),
            item = item
        )

        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckChanged,
            colors = CheckboxDefaults.colors(
                checkedColor = RadixTheme.colors.gray1,
                uncheckedColor = RadixTheme.colors.gray2,
                checkmarkColor = Color.White
            )
        )
    }
}
