@file:Suppress("MagicNumber")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Blue1 = Color(0xFF060F8F)
val Blue2 = Color(0xFF052CC0)

val Green1 = Color(0xFF00AB84)
val Green3 = Color(0xFF21FFBE)

val Pink1 = Color(0xFFCE0D98)

val Gray1 = Color(0xFF003057)
val Gray2 = Color(0xFF8A8FA4)
val Gray3 = Color(0xFFCED0D6)
val Gray4 = Color(0xFFE2E5ED)
val Gray5 = Color(0xFFF4F5F9)

// alert
val Orange1 = Color(0xFFF2AD21)
val Orange2 = Color(0xFFEC633E)
val Orange3 = Color(0xFFE59700)
val LightOrange = Color(0xFFFFF5E2)
val Red1 = Color(0xFFC82020)
val LightRed = Color(0xFFfcebeb)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)


data class RadixColors(
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundTertiary: Color,
    val backgroundTransparent: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textButton: Color,
    val icon: Color,
    val iconSecondary: Color,
    val iconTertiary: Color,
    val primaryButton: Color,
    val divider: Color,
    val border: Color,
    val textFieldBorder: Color,
    val textFieldFocusedBorder: Color,
    val textFieldBackground: Color,
    val toggleActive: Color,
    val ok: Color,
    val error: Color,
    val errorSecondary: Color,
    val warning: Color,
    val warningSecondary: Color,
    val card: Color
)

private val LightColorPalette = RadixColors(
    background = White,
    backgroundSecondary = Gray5,
    backgroundTertiary = Gray4,
    backgroundTransparent = White.copy(alpha = 0.3f),
    text = Gray1,
    textSecondary = Gray2,
    textTertiary = Gray3,
    textButton = Blue2,
    icon = Gray1,
    iconSecondary = Gray2,
    iconTertiary = Gray3,
    primaryButton = Blue2,
    divider = Gray4,
    border = Gray1,
    textFieldBorder = Gray4,
    textFieldFocusedBorder = Gray1,
    textFieldBackground = Gray5,
    toggleActive = Gray1,
    ok = Green1,
    error = Red1,
    errorSecondary = LightRed,
    warning = Orange3,
    warningSecondary = LightOrange,
    card = White
)

private val DarkColorPalette = RadixColors(
    background = Color(0xFF28292A),
    backgroundSecondary = Color(0xFF1E1F1F),
    backgroundTertiary = Color(0xFF404243),
    backgroundTransparent = Gray5.copy(alpha = 0.3f),
    text = Gray5,
    textSecondary = Gray4,
    textTertiary = Gray2,
    textButton = Color(0xFF90CAF9),
    icon = Gray5,
    iconSecondary = Gray4,
    iconTertiary = Gray2,
    primaryButton = Color(0xFF00C389),
    divider = Color(0xFF404243),
    border = Gray5,
    textFieldBorder = Color(0xFF797B7F),
    textFieldFocusedBorder = Gray5,
    textFieldBackground = Color(0xFF404243),
    toggleActive = Color(0xFF00C389),
    ok = Green1,
    error = Orange2,
    errorSecondary = LightRed,
    warning = Color(0xFFC47F00),
    warningSecondary = Color(0xFFE6DCCB),
    card = Color(0xFF28292A)
)

internal val LocalRadixColors = staticCompositionLocalOf<RadixColors> {
    error("No RadixColors provided")
}

@Composable
internal fun ProvideRadixColors(content: @Composable () -> Unit) {
    val isDarkMode = LocalRadixThemeConfig.current.isDarkTheme
    val colors = remember(isDarkMode) {
        if (isDarkMode) {
            DarkColorPalette
        } else {
            LightColorPalette
        }
    }
    CompositionLocalProvider(LocalRadixColors provides colors) {
        content()
    }
}