package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class RadixShapes(
    val circle: CornerBasedShape = CircleShape,
    val roundedRectXSmall: CornerBasedShape = RoundedCornerShape(4.dp),
    val roundedRectSmall: CornerBasedShape = RoundedCornerShape(8.dp),
    val roundedRectMedium: CornerBasedShape = RoundedCornerShape(12.dp),
    val roundedRectDefault: CornerBasedShape = RoundedCornerShape(16.dp),
    val roundedRectTopMedium: CornerBasedShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    val roundedRectBottomMedium: CornerBasedShape = RoundedCornerShape(bottomEnd = 12.dp, bottomStart = 12.dp),
    val roundedRectTopDefault: CornerBasedShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    val roundedTag: CornerBasedShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 36.dp,
        bottomStart = 4.dp,
        bottomEnd = 36.dp
    )
)

fun bubbleShape(
    density: Density,
    cornerRadius: Dp = 16.dp,
    arrowWidth: Dp = 19.dp,
    arrowHeight: Dp = 11.dp,
    arrowOffset: Dp = 40.dp,
): GenericShape {
    val cornerRadiusPx: Float
    val arrowWidthPx: Float
    val arrowHeightPx: Float
    val arrowOffsetPx: Float

    with(density) {
        cornerRadiusPx = cornerRadius.toPx()
        arrowWidthPx = arrowWidth.toPx()
        arrowHeightPx = arrowHeight.toPx()
        arrowOffsetPx = arrowOffset.toPx()
    }

    return GenericShape { size: Size, _: LayoutDirection ->

        addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(0f, 0f),
                    size = Size(size.width, size.height)
                ),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )

        val arrowXPoint = size.width - arrowOffsetPx

        moveTo(arrowXPoint, size.height)
        lineTo(arrowXPoint - arrowWidthPx / 2, size.height + arrowHeightPx)
        lineTo(arrowXPoint - arrowWidthPx, size.height)
    }
}
