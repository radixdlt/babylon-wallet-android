package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

fun Modifier.radixPlaceholder(
    visible: Boolean,
    shape: Shape? = null
) = composed {
    placeholder(
        visible = visible,
        color = RadixTheme.colors.gray4,
        shape = shape ?: RadixTheme.shapes.roundedRectDefault,
        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
    )
}
