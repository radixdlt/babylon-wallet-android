package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.presentation.account.composable.EmptyResourcesContent
import com.babylon.wallet.android.presentation.transfer.assets.ResourceTab

@Composable
fun FungibleResourcesColumn(
    modifier: Modifier,
    resources: Resources?,
    contentPadding: PaddingValues = PaddingValues(
        start = RadixTheme.dimensions.paddingMedium,
        end = RadixTheme.dimensions.paddingMedium,
        top = RadixTheme.dimensions.paddingLarge,
        bottom = 100.dp
    ),
    item: @Composable (index: Int, resource: Resource.FungibleResource) -> Unit
) {
    val xrdItem = resources?.xrd
    val restResources = resources?.nonXrdFungibles.orEmpty()

    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        fungibleResources(
            xrdItem = xrdItem,
            restOfFungibles = restResources
        ) { index, resource ->
            item(index = index, resource = resource)
        }
    }
}

fun LazyListScope.fungibleResources(
    modifier: Modifier = Modifier,
    xrdItem: Resource.FungibleResource?,
    restOfFungibles: List<Resource.FungibleResource>,
    item: @Composable (index: Int, resource: Resource.FungibleResource) -> Unit
) {
    if (xrdItem == null && restOfFungibles.isEmpty()) {
        item {
            EmptyResourcesContent(
                modifier = modifier.fillMaxWidth(),
                tab = ResourceTab.Tokens
            )
        }
    } else {
        if (xrdItem != null) {
            item {
                FungibleResourceCard(
                    modifier = modifier
                ) {
                    item(index = 0, resource = xrdItem)
                }
            }
        }

        itemsIndexed(
            items = restOfFungibles,
            key = { _, resource ->
                resource.resourceAddress
            },
            itemContent = { index, resource ->
                val topPadding = if (index == 0) RadixTheme.dimensions.paddingDefault else 0.dp
                val bottomPadding = if (index == restOfFungibles.size - 1) RadixTheme.dimensions.paddingDefault else 0.dp
                FungibleResourceCard(
                    modifier = modifier.padding(top = topPadding, bottom = bottomPadding),
                    itemIndex = index,
                    allItemsSize = restOfFungibles.size,
                    bottomContent = if (index != restOfFungibles.lastIndex) {
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
                    item(index = index, resource = resource)
                }
            }
        )
    }
}

@Composable
fun FungibleResourceCard(
    modifier: Modifier = Modifier,
    itemIndex: Int = 0,
    allItemsSize: Int = 1,
    shapeRadius: Dp = 12.dp,
    bottomContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shadowPadding = RadixTheme.dimensions.paddingDefault
    Card(
        modifier = modifier
            .drawWithContent {
                // Needed to remove shadow casted above of previous elements in the top side
                if (itemIndex != 0 && allItemsSize != 1) {
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
        shape = resourceCardShape(
            itemIndex = itemIndex,
            allItemsSize = allItemsSize,
            cornerRadius = shapeRadius
        ),
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
private fun resourceCardShape(
    itemIndex: Int,
    allItemsSize: Int,
    cornerRadius: Dp
): Shape = when {
    allItemsSize == 1 -> RoundedCornerShape(cornerRadius)
    itemIndex == 0 && allItemsSize > 1 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    itemIndex == allItemsSize - 1 && allItemsSize > 1 -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
    else -> RectangleShape
}
