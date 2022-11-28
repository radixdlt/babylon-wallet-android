package com.babylon.wallet.android.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetStatusBarColor(color: Color, useDarkIcons: Boolean) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(color, useDarkIcons)
}

fun Color.darken(percent: Float): Color {
    return copy(red = this.red * (1f - percent), blue = this.blue * (1f - percent), green = this.green * (1f - percent))
}