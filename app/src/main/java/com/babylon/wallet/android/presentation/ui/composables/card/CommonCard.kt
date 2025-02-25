package com.babylon.wallet.android.presentation.ui.composables.card

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

@Composable
fun CommonCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = RadixTheme.colors.defaultBackground,
    elevation: Dp = 4.dp,
    roundTopCorners: Boolean = true,
    roundBottomCorners: Boolean = true,
    removeTopShadow: Boolean = false,
    cornerSizeRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val topCorners = if (roundTopCorners) cornerSizeRadius else 0.dp
    val bottomCorners by animateDpAsState(
        targetValue = if (roundBottomCorners) cornerSizeRadius else 0.dp,
        label = "bottomCorners"
    )
    val shadowPadding = RadixTheme.dimensions.paddingDefault
    Card(
        modifier = modifier
            .drawWithContent {
                // Needed to remove shadow casted above of previous elements in the top side
                if (removeTopShadow) {
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
        shape = RoundedCornerShape(
            topStart = topCorners,
            topEnd = topCorners,
            bottomEnd = bottomCorners,
            bottomStart = bottomCorners
        ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun CommonCard(
    modifier: Modifier = Modifier,
    itemIndex: Int = 0,
    allItemsSize: Int = 1,
    backgroundColor: Color = RadixTheme.colors.defaultBackground,
    elevation: Dp = 4.dp,
    roundTopCorners: Boolean = true,
    roundBottomCorners: Boolean = true,
    cornerSizeRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    CommonCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        elevation = elevation,
        roundTopCorners = itemIndex == 0 && roundTopCorners,
        roundBottomCorners = itemIndex == allItemsSize - 1 && roundBottomCorners,
        cornerSizeRadius = cornerSizeRadius,
        removeTopShadow = itemIndex != 0 && allItemsSize != 1,
        content = content
    )
}

@Composable
fun CollapsibleCommonCard(
    modifier: Modifier = Modifier,
    isCollapsed: Boolean,
    collapsedItems: Int,
    backgroundColor: Color = RadixTheme.colors.defaultBackground,
    groupInnerPadding: Dp = 8.dp,
    elevation: Dp = 6.dp,
    cornerSizeRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val visibleCollapsedItems = collapsedItems.coerceAtMost(2)
    Box(modifier = modifier.padding(bottom = if (isCollapsed) groupInnerPadding * visibleCollapsedItems else 0.dp)) {
        if (isCollapsed) {
            repeat(visibleCollapsedItems) { index ->
                val collapsedIndex = visibleCollapsedItems - index
                val shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = cornerSizeRadius,
                    bottomEnd = cornerSizeRadius
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = groupInnerPadding * collapsedIndex)
                        .height(groupInnerPadding)
                        .padding(horizontal = groupInnerPadding * collapsedIndex)
                        .align(Alignment.BottomCenter)
                        .shadow(
                            elevation = elevation - collapsedIndex.dp,
                            shape = shape
                        )
                        .background(
                            color = backgroundColor,
                            shape = shape
                        )
                )
            }
        }

        CommonCard(
            modifier = Modifier
                .fillMaxWidth(),
            backgroundColor = backgroundColor,
            roundBottomCorners = isCollapsed,
            elevation = elevation,
            cornerSizeRadius = cornerSizeRadius
        ) {
            content()
        }
    }
}
