package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer

// NOT USED
fun Modifier.contentLoadingDefault(visible: Boolean): Modifier = composed {
    this.placeholder(
        visible = visible,
        color = RadixTheme.colors.gray4,
        shape = RadixTheme.shapes.roundedRectSmall,
        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.Black.copy(alpha = 0.2f)),
        contentFadeTransitionSpec = {
            tween(durationMillis = 400)
        }
    )
}
