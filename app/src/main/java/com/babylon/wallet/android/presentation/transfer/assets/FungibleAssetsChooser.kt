package com.babylon.wallet.android.presentation.transfer.assets

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.babylon.wallet.android.designsystem.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.listItemShape
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.presentation.transfer.SpendingAsset
import com.babylon.wallet.android.presentation.ui.composables.ImageSize
import com.babylon.wallet.android.presentation.ui.composables.rememberImageUrl
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import rdx.works.core.displayableQuantity

@Composable
fun FungibleAssetsChooser(
    modifier: Modifier = Modifier,
    resources: Resources?,
    selectedAssets: ImmutableSet<SpendingAsset>,
    onAssetSelectionChanged: (SpendingAsset, Boolean) -> Unit
) {
    val xrdResource = resources?.xrd
    val restResources = resources?.nonXrdFungibles.orEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = RadixTheme.dimensions.paddingDefault),
        contentPadding = PaddingValues(vertical = RadixTheme.dimensions.paddingDefault)
    ) {
        if (xrdResource != null) {
            item {
                ItemContainer(
                    modifier = Modifier.padding(top = RadixTheme.dimensions.paddingDefault),
                    shape = listItemShape()
                ) {
                    val isSelected = selectedAssets.any { it.address == xrdResource.resourceAddress }
                    Item(
                        modifier = Modifier.height(85.dp),
                        resource = xrdResource,
                        isSelected = isSelected,
                        onCheckChanged = {
                            val fungibleAsset = SpendingAsset.Fungible(resource = xrdResource)
                            onAssetSelectionChanged(fungibleAsset, it)
                        }
                    )
                }
            }
        }

        itemsIndexed(restResources) { index, resource ->
            val topPadding = if (index == 0) RadixTheme.dimensions.paddingDefault else 0.dp
            val shadowPadding = RadixTheme.dimensions.paddingDefault
            ItemContainer(
                modifier = Modifier
                    .padding(top = topPadding)
                    .drawWithContent {
                        // Needed to remove shadow casted above of previous elements in the top side
                        if (index != 0 && restResources.size != 1) {
                            val shadowPaddingPx = shadowPadding.toPx()
                            clipRect(
                                top = 0f,
                                left = -shadowPaddingPx,
                                right = size.width + shadowPaddingPx,
                                bottom = size.height + shadowPaddingPx
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    },
                shape = listItemShape(itemIndex = index, allItemsSize = restResources.size),
                bottomContent = if (index != restResources.lastIndex) {
                    {
                        Divider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = RadixTheme.colors.gray4
                        )
                    }
                } else {
                    null
                }
            ) {
                Item(
                    modifier = Modifier.height(83.dp),
                    resource = resource,
                    isSelected = selectedAssets.any { it.address == resource.resourceAddress },
                    onCheckChanged = {
                        val fungibleAsset = SpendingAsset.Fungible(resource = resource)
                        onAssetSelectionChanged(fungibleAsset, it)
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemContainer(
    modifier: Modifier = Modifier,
    shape: Shape,
    bottomContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = RadixTheme.colors.defaultBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column {
            content()

            bottomContent?.invoke()
        }
    }
}

@Composable
private fun Item(
    modifier: Modifier = Modifier,
    resource: Resource.FungibleResource,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onCheckChanged(!isSelected)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholder = rememberDrawablePainter(drawable = ColorDrawable(RadixTheme.colors.gray3.toArgb()))

        Spacer(modifier = Modifier.width(28.dp))

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
        Text(
            text = resource.amount.displayableQuantity(),
            style = RadixTheme.typography.secondaryHeader,
            color = RadixTheme.colors.gray1,
            maxLines = 2
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

        Spacer(modifier = Modifier.width(8.dp))
    }
}
