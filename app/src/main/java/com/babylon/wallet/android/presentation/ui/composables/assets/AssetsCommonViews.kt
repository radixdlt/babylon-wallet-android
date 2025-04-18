package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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

fun Modifier.dashedCircleBorder(color: Color) = composed {
    val strokeWidth = with(LocalDensity.current) { 1.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 3.dp.toPx() }
    then(
        Modifier.drawWithCache {
            onDrawBehind {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeInterval, strokeInterval), 0f)
                drawCircle(
                    color = color,
                    style = Stroke(strokeWidth, pathEffect = pathEffect)
                )
            }
        }
    )
}

@Suppress("MagicNumber")
fun Modifier.strokeLine() = composed {
    val strokeColor = RadixTheme.colors.gray3
    val strokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val strokeInterval = with(LocalDensity.current) { 6.dp.toPx() }
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeInterval, strokeInterval), 0f)
    then(
        Modifier.drawWithCache {
            onDrawBehind {
                drawLine(
                    color = strokeColor,
                    start = Offset(size.width - 150f, 0f),
                    end = Offset(size.width - 150f, size.height),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
        }
    )
}

@Composable
fun AssetsViewCheckBox(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Checkbox(
        modifier = modifier,
        checked = isSelected,
        onCheckedChange = onCheckChanged,
        colors = CheckboxDefaults.colors(
            checkedColor = RadixTheme.colors.gray1,
            uncheckedColor = RadixTheme.colors.gray2,
            checkmarkColor = Color.White
        )
    )
}
