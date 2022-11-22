package com.babylon.wallet.android.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetStatusBarColor(color: Color, useDarkIcons: Boolean) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(color, useDarkIcons)
}
