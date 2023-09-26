package com.babylon.wallet.android.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.modifier.applyIf
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun OnboardingGraphic(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val items = OnboardingAssetItem.initial(constraints)

        val offsetX = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()
        val draggableState = rememberDraggableState(onDelta = { delta ->
            coroutineScope.launch {
                offsetX.snapTo(offsetX.value + delta / 2)
            }
        })
        var imageRect by remember { mutableStateOf(Rect.Zero) }

        OnboardingItems(
            items = items,
            imageRect = imageRect,
            offsetX = offsetX
        )

        Image(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    imageRect = coordinates.boundsInParent()
                }
                .align(Alignment.Center)
                .offset {
                    IntOffset(x = offsetX.value.roundToInt(), y = 0)
                }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        coroutineScope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween()
                            )
                        }
                    }
                )
                .fillMaxWidth(fraction = 0.3f),
            painter = painterResource(id = R.drawable.ic_onboarding_phone),
            contentScale = ContentScale.FillWidth,
            contentDescription = null
        )
    }
}

@Composable
private fun rememberRotations(items: List<OnboardingAssetItem>): List<Animatable<Float, AnimationVector1D>> {
    val rotations = remember(items) { items.map { Animatable(0f) } }

    LaunchedEffect(items) {
        rotations.forEachIndexed { index, animatable ->
            launch {
                animatable.animateTo(
                    targetValue = items[index].rotationTimes * 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 90_000,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    return rotations
}

@Composable
private fun OnboardingItems(
    modifier: Modifier = Modifier,
    items: List<OnboardingAssetItem>,
    imageRect: Rect,
    offsetX: Animatable<Float, AnimationVector1D>
) {
    val rotations = rememberRotations(items = items)

    Box(modifier = modifier.fillMaxSize()) {
        items.forEachIndexed { index, item ->
            Image(
                modifier = Modifier
                    .offset { item.offset.value }
                    .rotate(rotations[index].value)
                    .align(Alignment.Center)
                    .blur(radius = 5.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded),
                painter = painterResource(id = item.resource),
                contentDescription = null
            )
        }
    }

    val radiusPx = with(LocalDensity.current){ 18.dp.toPx() }
    val clipPath = remember(imageRect, offsetX.value) {
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = imageRect.left + offsetX.value,
                    right = imageRect.right + offsetX.value,
                    top = imageRect.top,
                    bottom = imageRect.bottom,
                    topLeftCornerRadius = CornerRadius(radiusPx),
                    topRightCornerRadius = CornerRadius(radiusPx),
                    bottomLeftCornerRadius = CornerRadius(radiusPx),
                    bottomRightCornerRadius = CornerRadius(radiusPx)
                )
            )
        }
    }


    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                clipPath(clipPath) {
                    this@drawWithContent.drawContent()
                }
            }
    ) {
        items.forEachIndexed { index, item ->
            Image(
                modifier = Modifier
                    .offset { item.offset.value }
                    .rotate(rotations[index].value)
                    .align(Alignment.Center),
                painter = painterResource(id = item.resource),
                contentDescription = null
            )
        }
    }

}

private data class OnboardingAssetItem(
    @DrawableRes val resource: Int,
    val rotationTimes: Int,
    val offset: MutableState<IntOffset>
) {

    companion object {
        fun initial(constraints: Constraints): List<OnboardingAssetItem> {
            val width = constraints.minWidth
            val height = constraints.minHeight

            return listOf(
                OnboardingAssetItem(
                    resource = R.drawable.ic_onboarding_item_1,
                    rotationTimes = 3,
                    offset = mutableStateOf(
                        IntOffset((0.15f * width).roundToInt(), (-0.15 * height).roundToInt())
                    )
                ),
                OnboardingAssetItem(
                    resource = R.drawable.ic_onboarding_item_2,
                    rotationTimes = -5,
                    offset = mutableStateOf(
                        IntOffset((-0.25f * width).roundToInt(), (-0.08 * height).roundToInt())
                    )
                ),
                OnboardingAssetItem(
                    resource = R.drawable.ic_onboarding_item_3,
                    rotationTimes = 4,
                    offset = mutableStateOf(
                        IntOffset((0.05f * width).roundToInt(), (0.08 * height).roundToInt())
                    )
                ),
                OnboardingAssetItem(
                    resource = R.drawable.ic_onboarding_item_4,
                    rotationTimes = -4,
                    offset = mutableStateOf(
                        IntOffset((0.3f * width).roundToInt(), (0.2 * height).roundToInt())
                    )
                ),
                OnboardingAssetItem(
                    resource = R.drawable.ic_onboarding_item_5,
                    rotationTimes = -6,
                    offset = mutableStateOf(
                        IntOffset((-0.3f * width).roundToInt(), (0.25 * height).roundToInt())
                    )
                )
            )
        }
    }

}
