package com.babylon.wallet.android.designsystem.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.babylon.wallet.android.designsystem.darken

class RadixRippleTheme(private val color: Color, private val darkTheme: Boolean) : RippleTheme {

    @Composable
    override fun defaultColor(): Color {
        return color
    }

    @Composable
    override fun rippleAlpha(): RippleAlpha {
        return RippleTheme.defaultRippleAlpha(color.darken(0.1f), darkTheme)
    }
}
