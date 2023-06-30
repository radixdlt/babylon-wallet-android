package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

@Composable
fun ScrollableHeaderView(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    state: ScrollableHeaderViewScrollState = rememberScrollableHeaderViewScrollState(),
    topBarHeightPx: Int = 0,
) {
    val layoutScrollState = rememberScrollableState { state.drag(it) }
    Layout(
        modifier = modifier
            .scrollable(
                orientation = Orientation.Vertical,
                state = layoutScrollState,
            )
            .nestedScroll(state.nestedScrollConnectionHolder),
        content = {
            Box {
                header()
            }
            Box {
                content()
            }
        }
    ) { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            val offset = state.offset.roundToInt()
            val headerPlaceable =
                measurables[0].measure(constraints.copy(maxHeight = Constraints.Infinity))
            headerPlaceable.place(0, offset)
            val contentPlaceable =
                measurables[1].measure(constraints.copy(maxHeight = constraints.maxHeight))
            contentPlaceable.place(0, offset + headerPlaceable.height)
            // Make the header scrolling stop at the point right below the top bar.
            state.updateBounds(maxOffset = topBarHeightPx - headerPlaceable.height.toFloat())
        }
    }
}
