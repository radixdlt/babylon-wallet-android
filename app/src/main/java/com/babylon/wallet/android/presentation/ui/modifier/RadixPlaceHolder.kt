package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
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
        highlight = PlaceholderHighlight.shimmer(highlightColor = White),
        placeholderFadeTransitionSpec = placeholderFadeTransitionSpec,
        contentFadeTransitionSpec = contentFadeTransitionSpec
    )
}

@Suppress("MagicNumber")
fun Modifier.radixPlaceholderSimple(
    visible: Boolean,
    shape: Shape? = null,
    color: Color? = null,
) = composed {
    if (!visible) {
        return@composed this
    }

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    val brush = Brush.linearGradient(
        colors = (color ?: RadixTheme.colors.backgroundTertiary).toShimmerShades(),
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    background(
        brush = brush,
        shape = shape ?: RadixTheme.shapes.roundedRectDefault
    )
}

@Suppress("MagicNumber")
private fun Color.toShimmerShades() = listOf(copy(0.9f), copy(0.2f), copy(0.9f))
