@file:Suppress("MagicNumber", "LongParameterList", "CompositionLocalAllowlist")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Stable
class RadixColors(
    background: Color,
    backgroundSecondary: Color,
    backgroundTertiary: Color,
    text: Color,
    textSecondary: Color,
    textTertiary: Color,
    textButton: Color,
    icon: Color,
    iconSecondary: Color,
    iconTertiary: Color,
    primaryButton: Color,
    divider: Color,
    border: Color,
    ok: Color,
    error: Color,
    errorSecondary: Color,
    warning: Color,
    warningSecondary: Color,
    card: Color
) {
    var background by mutableStateOf(background)
        private set
    var backgroundSecondary by mutableStateOf(backgroundSecondary)
        private set
    var backgroundTertiary by mutableStateOf(backgroundTertiary)
        private set
    var text by mutableStateOf(text)
        private set
    var textSecondary by mutableStateOf(textSecondary)
        private set
    var textTertiary by mutableStateOf(textTertiary)
        private set
    var textButton by mutableStateOf(textButton)
        private set
    var icon by mutableStateOf(icon)
        private set
    var iconSecondary by mutableStateOf(iconSecondary)
        private set
    var iconTertiary by mutableStateOf(iconTertiary)
        private set
    var primaryButton by mutableStateOf(primaryButton)
        private set
    var divider by mutableStateOf(divider)
        private set
    var border by mutableStateOf(border)
        private set
    var ok by mutableStateOf(ok)
        private set
    var error by mutableStateOf(error)
        private set
    var errorSecondary by mutableStateOf(errorSecondary)
        private set
    var warning by mutableStateOf(warning)
        private set
    var warningSecondary by mutableStateOf(warningSecondary)
        private set
    var card by mutableStateOf(card)
        private set
}

private val LightColorPalette = RadixColors(
    background = White,
    backgroundSecondary = Gray5,
    backgroundTertiary = Gray4,
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
    ok = Green1,
    error = Orange2,
    errorSecondary = LightRed,
    warning = Orange3,
    warningSecondary = LightOrange,
    card = Color(0xFF28292A)
)

private val LocalRadixColors = staticCompositionLocalOf<RadixColors> {
    error("No RadixColors provided")
}

@Composable
fun ProvideRadixColors(content: @Composable () -> Unit) {
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

private val LocalRadixShapes = staticCompositionLocalOf<RadixShapes> {
    error("No RadixShapes provided")
}

@Composable
fun ProvideRadixShapes(content: @Composable () -> Unit) {
    val shapes = remember {
        RadixShapes()
    }
    CompositionLocalProvider(LocalRadixShapes provides shapes) {
        content()
    }
}

private val LocalRadixTypography = staticCompositionLocalOf<RadixTypography> {
    error("No RadixTypography provided")
}

@Composable
fun ProvideRadixTypography(content: @Composable () -> Unit) {
    val typography = remember {
        RadixTypography()
    }
    CompositionLocalProvider(LocalRadixTypography provides typography) {
        content()
    }
}

private val LocalRadixDimensions = staticCompositionLocalOf<RadixDimensions> {
    error("No RadixDimensions provided")
}

@Composable
fun ProvideRadixDimensions(content: @Composable () -> Unit) {
    val dimensions = remember {
        RadixDimensions()
    }
    CompositionLocalProvider(LocalRadixDimensions provides dimensions) {
        content()
    }
}

private val LocalRadixThemeConfig = staticCompositionLocalOf<RadixThemeConfig> {
    error("No RadixThemeConfig provided")
}

@Composable
fun ProvideRadixThemeConfig(
    config: RadixThemeConfig,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRadixThemeConfig provides config) {
        content()
    }
}

object RadixTheme {
    val colors: RadixColors
        @Composable get() = LocalRadixColors.current
    val typography: RadixTypography
        @Composable get() = LocalRadixTypography.current
    val dimensions: RadixDimensions
        @Composable get() = LocalRadixDimensions.current
    val shapes: RadixShapes
        @Composable get() = LocalRadixShapes.current
    val config: RadixThemeConfig
        @Composable get() = LocalRadixThemeConfig.current
}

@Composable
fun RadixWalletTheme(
    config: RadixThemeConfig = RadixThemeConfig(isSystemDarkTheme = isSystemInDarkTheme()),
    content: @Composable () -> Unit,
) {
    ProvideRadixThemeConfig(config = config) {
        ProvideRadixColors {
            ProvideRadixTypography {
                ProvideRadixDimensions {
                    ProvideRadixShapes {
                        MaterialTheme(
                            typography = RadixMaterialTypography,
                            content = content
                        )
                    }
                }
            }
        }
    }
}
