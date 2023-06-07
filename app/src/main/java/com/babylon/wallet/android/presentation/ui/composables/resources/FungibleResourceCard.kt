package com.babylon.wallet.android.presentation.ui.composables.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

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
        shape = fungibleResourceCardShape(
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
fun fungibleResourceCardShape(
    itemIndex: Int,
    allItemsSize: Int,
    cornerRadius: Dp
): Shape = when {
    allItemsSize == 1 -> RoundedCornerShape(cornerRadius)
    itemIndex == 0 && allItemsSize > 1 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    itemIndex == allItemsSize - 1 && allItemsSize > 1 -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
    else -> RectangleShape
}
