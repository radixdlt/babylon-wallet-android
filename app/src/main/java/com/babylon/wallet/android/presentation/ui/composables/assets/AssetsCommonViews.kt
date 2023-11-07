package com.babylon.wallet.android.presentation.ui.composables.assets

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
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
    then(Modifier.placeholder(
        visible = visible,
        color = RadixTheme.colors.gray4,
        shape = RoundedCornerShape(cornerSizeRadius),
        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
    ))
}
