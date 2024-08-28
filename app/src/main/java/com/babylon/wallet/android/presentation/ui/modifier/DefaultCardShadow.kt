package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.designsystem.theme.RadixTheme

fun Modifier.defaultCardShadow(
    elevation: Dp = 2.dp,
    shape: Shape? = null,
    color: Color? = null
) = composed {
    this.shadow(
        elevation = elevation,
        shape = shape ?: RadixTheme.shapes.roundedRectMedium,
        ambientColor = color ?: RadixTheme.colors.gray2,
        spotColor = color ?: RadixTheme.colors.gray2
    )
}
