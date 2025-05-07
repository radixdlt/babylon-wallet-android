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
    green1: Color,
    green3: Color,
    pink1: Color,

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
    borderSecondary: Color,
    error: Color,
    errorSecondary: Color,
    warning: Color,
    warningSecondary: Color,
    cardOnPrimary: Color,
    cardOnSecondary: Color
) {
    var green1 by mutableStateOf(green1)
        private set
    var green3 by mutableStateOf(green3)
        private set
    var pink1 by mutableStateOf(pink1)
        private set

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
    var borderSecondary by mutableStateOf(borderSecondary)
        private set
    var error by mutableStateOf(error)
        private set
    var errorSecondary by mutableStateOf(errorSecondary)
        private set
    var warning by mutableStateOf(warning)
        private set
    var warningSecondary by mutableStateOf(warningSecondary)
        private set
    var cardOnPrimary by mutableStateOf(cardOnPrimary)
        private set
    var cardOnSecondary by mutableStateOf(cardOnSecondary)
        private set

    fun copy(
        green1: Color = this.green1,
        green3: Color = this.green3,
        pink1: Color = this.pink1,
        background: Color = this.background,
        backgroundSecondary: Color = this.backgroundSecondary,
        backgroundTertiary: Color = this.backgroundTertiary,
        text: Color = this.text,
        textSecondary: Color = this.textSecondary,
        textTertiary: Color = this.textTertiary,
        textButton: Color = this.textButton,
        icon: Color = this.icon,
        iconSecondary: Color = this.iconSecondary,
        iconTertiary: Color = this.iconTertiary,
        primaryButton: Color = this.primaryButton,
        divider: Color = this.divider,
        border: Color = this.border,
        borderSecondary: Color = this.borderSecondary,
        error: Color = this.error,
        errorSecondary: Color = this.errorSecondary,
        warning: Color = this.warning,
        warningSecondary: Color = this.warningSecondary,
        cardOnPrimary: Color = this.cardOnPrimary,
        cardOnSecondary: Color = this.cardOnSecondary,
    ): RadixColors {
        return RadixColors(
            green1 = green1,
            green3 = green3,
            pink1 = pink1,
            background = background,
            backgroundSecondary = backgroundSecondary,
            backgroundTertiary = backgroundTertiary,
            text = text,
            textSecondary = textSecondary,
            textTertiary = textTertiary,
            textButton = textButton,
            icon = icon,
            iconSecondary = iconSecondary,
            iconTertiary = iconTertiary,
            primaryButton = primaryButton,
            divider = divider,
            border = border,
            borderSecondary = borderSecondary,
            error = error,
            errorSecondary = errorSecondary,
            warning = warning,
            warningSecondary = warningSecondary,
            cardOnPrimary = cardOnPrimary,
            cardOnSecondary = cardOnSecondary
        )
    }
}

private val LightColorPalette = RadixColors(
    green1 = Green1,
    green3 = Green3,
    pink1 = Pink1,
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
    borderSecondary = Gray3,
    error = Red1,
    errorSecondary = LightRed,
    warning = Orange3,
    warningSecondary = LightOrange,
    cardOnPrimary = Gray4,
    cardOnSecondary = White
)

private val DarkColorPalette = LightColorPalette.copy(
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
    borderSecondary = Gray3,
    error = Red1,
    errorSecondary = LightRed,
    warning = Orange3,
    cardOnPrimary = Color(0xFF1E1F1F),
    cardOnSecondary = Color(0xFF28292A)
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
