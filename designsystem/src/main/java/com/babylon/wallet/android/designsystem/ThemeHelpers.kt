package com.babylon.wallet.android.designsystem

import androidx.compose.ui.graphics.Color

fun Color.darken(percent: Float): Color {
    return copy(red = this.red * (1f - percent), blue = this.blue * (1f - percent), green = this.green * (1f - percent))
}
