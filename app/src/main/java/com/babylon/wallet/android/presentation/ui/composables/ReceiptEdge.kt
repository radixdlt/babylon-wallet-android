package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ReceiptEdge(modifier: Modifier, color: Color, topEdge: Boolean = false) {
    val density = LocalDensity.current
    val expectedWidth = with(density) { 12.dp.toPx() }
    val triangleHeight = with(density) { 6.dp.toPx() }
    Box(
        modifier = modifier
            .fillMaxWidth().height(6.dp)
            .drawWithCache {
                val triangles = (size.width / expectedWidth).roundToInt()
                val triangleWidth = size.width / triangles
                val startY = if (topEdge) triangleHeight else 0f
                val y1 = if (topEdge) 0f else triangleHeight
                val y2 = if (topEdge) triangleHeight else 0f
                val path = Path().apply {
                    moveTo(0f, startY)
                    repeat(triangles) { i ->
                        val startX = i * triangleWidth
                        lineTo(startX + 0.5f * triangleWidth, y1)
                        lineTo(startX + triangleWidth, y2)
                    }
                    close()
                }
                onDrawBehind {
                    drawPath(path, color = color)
                }
            }
    )
}
