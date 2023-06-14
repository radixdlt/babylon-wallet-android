package com.babylon.wallet.android.presentation.ui.composables.resources

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import rdx.works.core.displayableQuantity

@Composable
fun FungibleResourceItem(
    resource: Resource.FungibleResource,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit) = {
        Spacer(modifier = Modifier.width(28.dp))
    }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.width(28.dp))

        val placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))
        AsyncImage(
            modifier = Modifier
                .size(44.dp)
                .background(RadixTheme.colors.gray3, shape = RadixTheme.shapes.circle)
                .clip(RadixTheme.shapes.circle),
            model = if (resource.isXrd) {
                R.drawable.ic_xrd_token
            } else {
                rememberImageUrl(fromUrl = resource.iconUrl.toString(), size = ImageSize.MEDIUM)
            },
            placeholder = placeholder,
            fallback = placeholder,
            error = placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))
        Text(
            modifier = Modifier.weight(1f),
            text = resource.displayTitle,
            style = RadixTheme.typography.body2HighImportance,
            color = RadixTheme.colors.gray1,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingMedium))

        resource.amount?.let { amount ->
            Text(
                text = amount.displayableQuantity(),
                style = RadixTheme.typography.secondaryHeader,
                color = RadixTheme.colors.gray1,
                maxLines = 2
            )
        }

        trailingContent()
    }
}

@Composable
fun SelectableFungibleResourceItem(
    modifier: Modifier = Modifier,
    resource: Resource.FungibleResource,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    FungibleResourceItem(
        modifier = modifier,
        resource = resource,
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = RadixTheme.colors.gray1,
                    uncheckedColor = RadixTheme.colors.gray2,
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(RadixTheme.dimensions.paddingSmall))
        }
    )
}
