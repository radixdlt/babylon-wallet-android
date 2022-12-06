@file:Suppress("ReturnCount", "ComplexCondition")
package com.babylon.wallet.android.presentation.ui.composables

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.withSign

// Jakub: Adopted from 3rd party library, can't recall lib name
// The minimum velocity a fling needs to be considered for header snap-to-top/bottom behavior.
// Sometimes lifting your finger off the screen while slowly scrolling triggers fling velocity
// in the API because of a fast, tiny movement in the finger on the screen while lifting it.
// We want to ignore that type of event because it's not an intended fling by the user.
private const val SCROLL_FLING_SENSITIVITY = 190f

@Composable
fun rememberScrollableHeaderViewScrollState(): ScrollableHeaderViewScrollState {
    val scope = rememberCoroutineScope()
    val saver = remember {
        ScrollableHeaderViewScrollState.Saver(scope = scope)
    }
    return rememberSaveable(
        saver = saver
    ) {
        ScrollableHeaderViewScrollState(scope = scope)
    }
}

@Stable
class ScrollableHeaderViewScrollState(
    private val scope: CoroutineScope,
    initialOffset: Float = 0f,
    initialMaxOffset: Float = 0f,
) {
    companion object {
        // Suppress function name should start with a lowercase letter.
        @Suppress("FunctionName")
        fun Saver(
            scope: CoroutineScope,
        ): Saver<ScrollableHeaderViewScrollState, *> = listSaver(
            save = {
                listOf(it.offset, it._maxOffset.value)
            },
            restore = {
                ScrollableHeaderViewScrollState(
                    scope = scope,
                    initialOffset = it[0],
                    initialMaxOffset = it[1],
                )
            }
        )
    }

    /**
     * The maximum value for nested scroll Content to translate.
     */
    @get:FloatRange(from = 0.0)
    val maxOffset: Float
        get() = _maxOffset.value.absoluteValue

    /**
     * The current value for nested scroll Content to translate.
     */
    @get:FloatRange(from = 0.0)
    val offset: Float
        get() = _offset.value

    internal val nestedScrollConnectionHolder = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // If we're dragging in the negative direction (scrolling down to see content below).
            return if (available.y < 0 && source == NestedScrollSource.Drag)
                Offset(0f, drag(available.y))
            else
                Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (available.y > 0 && source == NestedScrollSource.Drag)
                Offset(0f, drag(available.y))
            else
                Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return Velocity(0f, fling(available.y))
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            return Velocity(0f, fling(available.y))
        }
    }

    private var lastDragDelta = 0f
    private val _offset = Animatable(initialOffset)
    private val _maxOffset = mutableStateOf(initialMaxOffset)

    private suspend fun snapTo(value: Float) {
        _offset.snapTo(value.coerceIn(_maxOffset.value, 0f))
    }

    private suspend fun fling(velocity: Float): Float {
        val realVelocity = velocity.withSign(lastDragDelta)
        lastDragDelta = 0f

        if (velocity == 0f) {
            return velocity
        }

        // if we're between 0 and max, hog it all
        val offsetBetweenZeroAndMax = offset > _maxOffset.value && offset < 0f
        val offsetAtZeroAndScrollingDown = offset == 0f && velocity < 0

        // Ignore drags that end with an accidental fling before reaching 0f or maxOffset.
        if (offsetBetweenZeroAndMax && velocity.absoluteValue < SCROLL_FLING_SENSITIVITY) {
            return velocity
        }

        return if (offsetBetweenZeroAndMax || offsetAtZeroAndScrollingDown) {
            _offset.animateTo(
                targetValue = if (realVelocity < 0) _maxOffset.value else 0f,
                initialVelocity = realVelocity,
            ).endState.velocity.let {
                velocity
            }
        } else {
            0f
        }
    }

    fun drag(delta: Float): Float {
        if (delta != 0f) {
            // Record the scroll direction for a possible fling gesture later,
            // because in the NestedScrollConnection fling callbacks
            // the velocity sign/direction isn't always accurate.
            lastDragDelta = delta
        }

        return if (delta < 0 && offset > _maxOffset.value || delta > 0 && offset < 0f) {
            scope.launch {
                snapTo(offset + delta)
            }
            delta
        } else {
            0f
        }
    }

    fun updateBounds(maxOffset: Float) {
        _maxOffset.value = maxOffset
        _offset.updateBounds(maxOffset, 0f)
    }
}
