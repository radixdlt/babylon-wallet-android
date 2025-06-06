package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.White
import com.babylon.wallet.android.presentation.ui.RadixWalletPreviewTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlideToSignButton(
    title: String,
    enabled: Boolean,
    isSubmitting: Boolean,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColor = if (enabled) {
        RadixTheme.colors.primaryButton
    } else {
        RadixTheme.colors.backgroundTertiary
    }
    val textColor = if (enabled) {
        White
    } else {
        RadixTheme.colors.textTertiary
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(buttonColor, shape = RadixTheme.shapes.circle)
            .clip(RadixTheme.shapes.circle)
    ) {
        var textPositionInRoot by remember { mutableStateOf(Offset.Zero) }
        val density = LocalDensity.current
        val swipeCompleteThreshold = 0.7f
        val indicatorSize = 48.dp
        val indicatorPadding = 2.dp
        val indicatorWidthPx = with(density) { indicatorSize.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() - 2 * indicatorPadding.toPx() } - indicatorWidthPx

        val decay = rememberSplineBasedDecay<Float>()
        val draggableState = remember(maxWidthPx, decay) {
            AnchoredDraggableState(
                initialValue = ButtonSliderPosition.Start,
                anchors = DraggableAnchors {
                    ButtonSliderPosition.Start at 0f
                    ButtonSliderPosition.End at maxWidthPx * 2
                },
                positionalThreshold = { distance: Float -> distance * swipeCompleteThreshold },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = tween(),
                decayAnimationSpec = decay
            )
        }
        // listen for complete swipe
        LaunchedEffect(draggableState) {
            snapshotFlow { draggableState.currentValue }.distinctUntilChanged().filter { it == ButtonSliderPosition.End }.collect {
                onSwipeComplete()
            }
        }
        LaunchedEffect(isSubmitting) {
            if (!isSubmitting && draggableState.currentValue == ButtonSliderPosition.End) {
                draggableState.animateTo(ButtonSliderPosition.Start)
            }
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .onGloballyPositioned { coordinates ->
                    textPositionInRoot = coordinates.positionInRoot()
                }
                .drawWithCache {
                    // cover text when indicator moves over it
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            buttonColor,
                            size = Size(
                                draggableState
                                    .requireOffset()
                                    .roundToInt() + indicatorWidthPx - textPositionInRoot.x,
                                size.height
                            )
                        )
                    }
                },
            text = title,
            style = RadixTheme.typography.body1Header,
            color = textColor
        )

        val gradient = RadixTheme.gradients.slideToSignGradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    // display gradient as indicator moves
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.linearGradient(gradient),
                            size = if (isSubmitting) {
                                size
                            } else if (draggableState.requireOffset() > 0f) {
                                Size(draggableState.requireOffset() + indicatorWidthPx / 2, size.height)
                            } else {
                                Size.Zero
                            },
                            alpha = draggableState.requireOffset() / maxWidthPx
                        )
                    }
                }
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        draggableState
                            .requireOffset()
                            .roundToInt()
                            .coerceIn(0, maxWidthPx.roundToInt()),
                        0
                    )
                }
                .padding(indicatorPadding)
                .size(indicatorSize)
                .background(RadixTheme.colors.card, RadixTheme.shapes.circle)
                .applyIf(
                    enabled && !isSubmitting,
                    Modifier.anchoredDraggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                    )
                )
        ) {
            val crossedSwipeThreshold = draggableState.requireOffset() / maxWidthPx >= swipeCompleteThreshold
            if (isSubmitting) {
                // loading in the indicator if we are submitting
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center),
                    color = RadixTheme.colors.icon,
                    strokeWidth = 2.dp
                )
            } else {
                // show Radix logo when we crossed swipe threshold to signal action will happen when indicator is released
                AnimatedVisibility(modifier = Modifier.align(Alignment.Center), visible = !crossedSwipeThreshold) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = if (enabled) RadixTheme.colors.icon else RadixTheme.colors.iconTertiary
                    )
                }
                AnimatedVisibility(modifier = Modifier.align(Alignment.Center), visible = crossedSwipeThreshold) {
                    Icon(
                        modifier = Modifier.align(Alignment.Center),
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_radix),
                        contentDescription = null,
                        tint = RadixTheme.colors.icon
                    )
                }
            }
        }
    }
}

enum class ButtonSliderPosition {
    Start, End
}

@Preview
@Composable
fun AccountContentPreviewLight() {
    RadixWalletPreviewTheme {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = true,
            isSubmitting = false,
            onSwipeComplete = { }
        )
    }
}

@Preview
@Composable
fun AccountContentPreviewDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = true,
            isSubmitting = false,
            onSwipeComplete = { }
        )
    }
}

@Preview
@Composable
fun AccountContentPreviewDisabledLight() {
    RadixWalletPreviewTheme {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = false,
            isSubmitting = false,
            onSwipeComplete = { }
        )
    }
}

@Preview
@Composable
fun AccountContentPreviewDisabledDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = false,
            isSubmitting = false,
            onSwipeComplete = { }
        )
    }
}

@Preview
@Composable
fun AccountContentPreviewSubmittingLight() {
    RadixWalletPreviewTheme {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = true,
            isSubmitting = true,
            onSwipeComplete = { }
        )
    }
}

@Preview
@Composable
fun AccountContentPreviewSubmittingDark() {
    RadixWalletPreviewTheme(enableDarkTheme = true) {
        SlideToSignButton(
            title = stringResource(id = R.string.interactionReview_slideToSign),
            enabled = true,
            isSubmitting = true,
            onSwipeComplete = { }
        )
    }
}
