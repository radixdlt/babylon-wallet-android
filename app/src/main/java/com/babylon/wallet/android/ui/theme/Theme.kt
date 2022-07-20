package com.babylon.wallet.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
//    primary = Purple200,
//    primaryVariant = Purple700,
//    secondary = Teal200
)

private val LightColorPalette = lightColors(
//    primary = White,
//    primaryVariant = Purple700,
//    secondary = Teal200,
//    onPrimary = Color.Black,

    primary = White,
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF03DAC6),
    secondaryVariant = Color(0xFF018786),
    onSecondary = Color(0xFFFF4500),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFF00),
    onSurface = Color(0xFFADD8E6),
    error = Color(0xFFB00020),
    onError = Color(0xFFCCCCCC),

//    secondary: Color = Color(0xFF03DAC6),
//    secondaryVariant: Color = Color(0xFF018786),
//    background: Color = Color.White,
//    surface: Color = Color.White,
//    error: Color = Color(0xFFB00020),
//    onPrimary: Color = Color.White,
//    onSecondary: Color = Color.Black,
//    onBackground: Color = Color.Black,
//    onSurface: Color = Color.Black,
//    onError: Color = Color.White

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun BabylonWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}