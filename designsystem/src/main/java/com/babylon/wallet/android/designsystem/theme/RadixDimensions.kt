package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RadixDimensions(
    val paddingXXSmall: Dp = 2.dp,
    val paddingXSmall: Dp = 4.dp,
    val paddingSmall: Dp = 8.dp,
    val paddingMedium: Dp = 12.dp,
    val paddingDefault: Dp = 16.dp,
    val paddingSemiLarge: Dp = 20.dp,
    val paddingLarge: Dp = 24.dp,
    val paddingXLarge: Dp = 32.dp,
    val paddingXXLarge: Dp = 40.dp,
    val paddingXXXLarge: Dp = 48.dp,
)

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + other.calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection) + other.calculateEndPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        bottom = calculateBottomPadding() + other.calculateBottomPadding()
    )
}
