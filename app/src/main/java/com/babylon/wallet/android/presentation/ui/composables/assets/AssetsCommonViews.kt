package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

fun Modifier.assetOutlineBorder() = composed {
    then(Modifier.border(1.dp, RadixTheme.colors.gray4, RadixTheme.shapes.roundedRectMedium))
}

fun Modifier.assetPlaceholder(
    visible: Boolean = true,
    cornerSizeRadius: Dp = 12.dp
) = composed {
    then(
        Modifier.placeholder(
            visible = visible,
            color = RadixTheme.colors.gray4,
            shape = RoundedCornerShape(cornerSizeRadius),
            highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
        )
    )
}

@Composable
fun AssetCard(
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
    val isLast = itemIndex == allItemsSize - 1
    val topCorners = if (itemIndex == 0 && roundTopCorners) cornerSizeRadius else 0.dp
    val bottomCorners by animateDpAsState(
        targetValue = if (isLast && roundBottomCorners) cornerSizeRadius else 0.dp,
        label = "bottomCorners"
    )
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
fun CollapsibleAssetCard(
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

        AssetCard(
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

@Composable
fun AssetsViewCheckBox(
    modifier: Modifier = Modifier,
    resourceAddress: String,
    action: AssetsViewAction.Selection
) {
    val isSelected = remember(action, resourceAddress) {
        action.isSelected(resourceAddress)
    }
    Checkbox(
        modifier = modifier,
        checked = isSelected,
        onCheckedChange = {
            action.onResourceCheckChanged(resourceAddress, it)
        },
        colors = CheckboxDefaults.colors(
            checkedColor = RadixTheme.colors.gray1,
            uncheckedColor = RadixTheme.colors.gray2,
            checkmarkColor = Color.White
        )
    )
}
