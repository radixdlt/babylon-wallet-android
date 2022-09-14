@file:Suppress("MagicNumber")
package com.babylon.wallet.android.presentation.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// TODO - IMPORTANT
//  The resource-theme of the app has been switched from Material 2 to Material 3.
//  The reason was to fix the bug that caused a crash with the biometric dialog on Android 6 to 8.1.
//  Although the resource-theme is set to Material 3,
//  the rest of the UI components and the UI theme is still in Material 2.
//  Once we have a design system we should update the Theme file following the Material Design 3 guidelines
//  and switch the UI components from Material 2 to 3 if it is needed!
//  For samples have a look at Jetsurvey and JetNews

// private val DarkColorPalette = darkColors(
//    primary = Purple200,
//    primaryVariant = Purple700,
//    secondary = Teal200
// )

private val LightColorPalette = lightColors(
//    primary = White,
//    primaryVariant = Purple700,
//    secondary = Teal200,
//    onPrimary = Color.Black,

    primary = White,
    primaryVariant = Color(0xFF3700B3),
    onPrimary = Color(0xFF000000),
    secondary = RadixBackground,
    secondaryVariant = Color(0xFF018786),
    onSecondary = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = RadixCardBackground,
    onSurface = Color(0xFF535353), // Color(0xFF000000),
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
//    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // TODO we don't support dark theme at the moment,
    //  if later is needed, we enable it.
    //  Otherwise in a device with dark theme
    //  the app's colors are different and not consistent
    val colors = LightColorPalette // if (darkTheme) {
//        DarkColorPalette
//    } else {
//        LightColorPalette
//    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
