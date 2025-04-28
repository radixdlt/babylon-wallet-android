package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.sign

@Composable
fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    pageIndexMapping: (Int) -> Int = { it },
    activeColor: Color = LocalContentColor.current,
    inactiveColor: Color = LocalContentColor.current.copy(alpha = 0.4f),
    activeIndicatorWidth: Dp = 8.dp,
    activeIndicatorHeight: Dp = activeIndicatorWidth,
    inactiveIndicatorWidth: Dp = 8.dp,
    inactiveIndicatorHeight: Dp = inactiveIndicatorWidth,
    spacing: Dp = activeIndicatorWidth,
    indicatorShape: Shape = CircleShape,
) {
    val activeIndicatorWidthPx = LocalDensity.current.run { activeIndicatorWidth.roundToPx() }
    val spacingPx = LocalDensity.current.run { spacing.roundToPx() }

    val inactiveWidth = inactiveIndicatorWidth.coerceAtMost(activeIndicatorWidth)
    val inactiveHeight = inactiveIndicatorHeight.coerceAtMost(activeIndicatorHeight)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val indicatorModifier = Modifier
                .size(
                    width = activeIndicatorWidth,
                    height = activeIndicatorHeight
                )
                .padding(
                    horizontal = (activeIndicatorWidth - inactiveWidth) / 2,
                    vertical = (activeIndicatorHeight - inactiveHeight) / 2
                )
                .background(color = inactiveColor, shape = indicatorShape)

            repeat(pagerState.pageCount) {
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val position = pageIndexMapping(pagerState.currentPage)
                    val offset = pagerState.currentPageOffsetFraction
                    val next = pageIndexMapping(pagerState.currentPage + offset.sign.toInt())
                    val scrollPosition = ((next - position) * offset.absoluteValue + position)
                        .coerceIn(
                            0f,
                            (pagerState.pageCount - 1)
                                .coerceAtLeast(0)
                                .toFloat()
                        )

                    IntOffset(
                        x = ((spacingPx + activeIndicatorWidthPx) * scrollPosition).toInt(),
                        y = 0
                    )
                }
                .size(width = activeIndicatorWidth, height = activeIndicatorHeight)
                .then(
                    if (pagerState.pageCount > 0) {
                        Modifier.background(
                            color = activeColor,
                            shape = indicatorShape,
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}
