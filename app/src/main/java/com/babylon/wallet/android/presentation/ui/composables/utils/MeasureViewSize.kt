package com.babylon.wallet.android.presentation.ui.composables.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.DpSize

/**
 * Measure the size of a view and pass it to the content composable.
 *
 * Check [SubcomposeLayout] for more information on how to to use the values
 * calculated during the measurement as params for the composition of the children.
 *
 * @param viewToMeasure The view to measure.
 * @param modifier The modifier to apply to the layout.
 * @param content The content composable that will receive the measured size.
 */
@Composable
fun MeasureViewSize(
    viewToMeasure: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (DpSize) -> Unit,
) {
    SubcomposeLayout(
        modifier = modifier
    ) { constraints ->
        val measuredSize = subcompose("viewToMeasure") {
            viewToMeasure()
        }.first().measure(constraints).let {
            DpSize(
                width = it.width.toDp(),
                height = it.height.toDp()
            )
        }

        val contentPlaceable = subcompose("content") {
            content(measuredSize)
        }.firstOrNull()?.measure(constraints)

        layout(contentPlaceable?.width ?: 0, contentPlaceable?.height ?: 0) {
            contentPlaceable?.place(0, 0)
        }
    }
}
