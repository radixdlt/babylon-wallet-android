package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.designsystem.theme.GradientBrand2
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SlideToSignButton(
    enabled: Boolean,
    isSubmitting: Boolean,
    onSwipeComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonColor = if (enabled) RadixTheme.colors.blue1 else RadixTheme.colors.gray4
    val textColor = if (enabled) RadixTheme.colors.white else RadixTheme.colors.gray3
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(buttonColor, shape = RadixTheme.shapes.circle)
            .clip(RadixTheme.shapes.circle)
    ) {
        var textPositionInRoot by remember { mutableStateOf(Offset.Zero) }
        val swipeCompleteThreshold = 0.7f
        val indicatorSize = 48.dp
        val indicatorPadding = 2.dp
        val indicatorWidthPx = with(LocalDensity.current) { indicatorSize.toPx() }
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() - 2 * indicatorPadding.toPx() } - indicatorWidthPx
        val swipeableState = rememberSwipeableState(initialValue = ButtonSliderPosition.Start)
        // listen for complete swipe
        LaunchedEffect(swipeableState) {
            snapshotFlow { swipeableState.currentValue }.distinctUntilChanged().filter { it == ButtonSliderPosition.End }.collect {
                onSwipeComplete()
                swipeableState.animateTo(ButtonSliderPosition.Start)
            }
        }
        val anchors = mapOf(0f to ButtonSliderPosition.Start, maxWidthPx to ButtonSliderPosition.End)
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
                            size = Size(swipeableState.offset.value + indicatorWidthPx - textPositionInRoot.x, size.height)
                        )
                    }
                },
            text = stringResource(id = R.string.transactionReview_slideToSign),
            style = RadixTheme.typography.body1Header,
            color = textColor
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    // display gradient as indicator moves
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = GradientBrand2,
                            size = if (swipeableState.offset.value > 0f) {
                                Size(swipeableState.offset.value + indicatorWidthPx / 2, size.height)
                            } else {
                                Size.Zero
                            },
                            alpha = swipeableState.offset.value / maxWidthPx
                        )
                    }
                }
        )
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        swipeableState.offset.value
                            .roundToInt()
                            .coerceIn(0, maxWidthPx.roundToInt()),
                        0
                    )
                }
                .padding(indicatorPadding)
                .size(indicatorSize)
                .background(RadixTheme.colors.white, RadixTheme.shapes.circle)
                .applyIf(
                    enabled && !isSubmitting,
                    Modifier.swipeable(
                        state = swipeableState,
                        anchors = anchors,
                        orientation = Orientation.Horizontal,
                        thresholds = { _, _ ->
                            FractionalThreshold(swipeCompleteThreshold)
                        },
                    )
                )
        ) {
            val crossedSwipeThreshold = swipeableState.offset.value / maxWidthPx >= swipeCompleteThreshold
            if (isSubmitting) {
                // loading in the indicator if we are submitting
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center),
                    color = RadixTheme.colors.blue1,
                    strokeWidth = 2.dp
                )
            } else {
                // show Radix logo when we crossed swipe threshold to signal action will happen when indicator is released
                AnimatedVisibility(modifier = Modifier.align(Alignment.Center), visible = !crossedSwipeThreshold) {
                    Icon(
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = RadixTheme.colors.blue1
                    )
                }
                AnimatedVisibility(modifier = Modifier.align(Alignment.Center), visible = crossedSwipeThreshold) {
                    Icon(
                        modifier = Modifier.align(Alignment.Center),
                        painter = painterResource(id = com.babylon.wallet.android.designsystem.R.drawable.ic_radix),
                        contentDescription = null,
                        tint = RadixTheme.colors.blue1
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
fun AccountContentPreview() {
    RadixWalletTheme {
        SlideToSignButton(enabled = true, isSubmitting = false, onSwipeComplete = { })
    }
}

@Preview
@Composable
fun AccountContentPreviewDisabled() {
    RadixWalletTheme {
        SlideToSignButton(enabled = false, isSubmitting = false, onSwipeComplete = { })
    }
}

@Preview
@Composable
fun AccountContentPreviewSubmitting() {
    RadixWalletTheme {
        SlideToSignButton(enabled = true, isSubmitting = true, onSwipeComplete = { })
    }
}
