package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
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
    shape: Shape? = null,
    color: Color? = null,
    placeholderFadeTransitionSpec: @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<Float> = { spring() },
    contentFadeTransitionSpec: @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<Float> = { spring() },
) = composed {
    this.placeholder(
        visible = visible,
        color = color ?: RadixTheme.colors.backgroundTertiary,
        shape = shape ?: RadixTheme.shapes.roundedRectDefault,
        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White),
        placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
        contentFadeTransitionSpec = contentFadeTransitionSpec
    )
}
