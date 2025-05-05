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
    blue2: Color,
    green1: Color,
    green3: Color,
    pink1: Color,
    gray1: Color,
    gray2: Color,
    gray3: Color,
    gray4: Color,
    gray5: Color,
    orange1: Color,
    orange2: Color,
    orange3: Color,
    lightOrange: Color,
    red1: Color,
    lightRed: Color,
    white: Color,

    background: Color,
    backgroundSecondary: Color,
    backgroundTertiary: Color,
    text: Color,
    textSecondary: Color,
    textButton: Color,
    icon: Color,
    iconSecondary: Color,
    primaryButton: Color,
    divider: Color,
    border: Color,
    borderSecondary: Color,
    error: Color,
    warning: Color,
    cardOnPrimary: Color,
    cardOnSecondary: Color
) {
    var blue2 by mutableStateOf(blue2)
        private set
    var green1 by mutableStateOf(green1)
        private set
    var green3 by mutableStateOf(green3)
        private set
    var pink1 by mutableStateOf(pink1)
        private set
    var gray1 by mutableStateOf(gray1)
        private set
    var gray2 by mutableStateOf(gray2)
        private set
    var gray3 by mutableStateOf(gray3)
        private set
    var gray4 by mutableStateOf(gray4)
        private set
    var gray5 by mutableStateOf(gray5)
        private set
    var orange1 by mutableStateOf(orange1)
        private set
    var orange2 by mutableStateOf(orange2)
        private set
    var orange3 by mutableStateOf(orange3)
        private set
    var lightOrange by mutableStateOf(lightOrange)
        private set
    var red1 by mutableStateOf(red1)
        private set
    var lightRed by mutableStateOf(lightRed)
        private set
    var white by mutableStateOf(white)
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
    var textButton by mutableStateOf(textButton)
        private set
    var icon by mutableStateOf(icon)
        private set
    var iconSecondary by mutableStateOf(iconSecondary)
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
    var warning by mutableStateOf(warning)
        private set
    var cardOnPrimary by mutableStateOf(cardOnPrimary)
        private set
    var cardOnSecondary by mutableStateOf(cardOnSecondary)
        private set

    fun copy(
        blue2: Color = this.blue2,
        green1: Color = this.green1,
        green3: Color = this.green3,
        pink1: Color = this.pink1,
        gray1: Color = this.gray1,
        gray2: Color = this.gray2,
        gray3: Color = this.gray3,
        gray4: Color = this.gray4,
        gray5: Color = this.gray5,
        orange1: Color = this.orange1,
        orange2: Color = this.orange2,
        orange3: Color = this.orange3,
        lightOrange: Color = this.lightOrange,
        red1: Color = this.red1,
        lightRed: Color = this.lightRed,
        white: Color = this.white,
        background: Color = this.background,
        backgroundSecondary: Color = this.backgroundSecondary,
        backgroundTertiary: Color = this.backgroundTertiary,
        text: Color = this.text,
        textSecondary: Color = this.textSecondary,
        textButton: Color = this.textButton,
        icon: Color = this.icon,
        iconSecondary: Color = this.iconSecondary,
        primaryButton: Color = this.primaryButton,
        divider: Color = this.divider,
        border: Color = this.border,
        borderSecondary: Color = this.borderSecondary,
        error: Color = this.error,
        warning: Color = this.warning,
        cardOnPrimary: Color = this.cardOnPrimary,
        cardOnSecondary: Color = this.cardOnSecondary,
    ): RadixColors {
        return RadixColors(
            blue2 = blue2,
            green1 = green1,
            green3 = green3,
            pink1 = pink1,
            gray1 = gray1,
            gray2 = gray2,
            gray3 = gray3,
            gray4 = gray4,
            gray5 = gray5,
            orange1 = orange1,
            orange2 = orange2,
            orange3 = orange3,
            lightOrange = lightOrange,
            red1 = red1,
            lightRed = lightRed,
            white = white,
            background = background,
            backgroundSecondary = backgroundSecondary,
            backgroundTertiary = backgroundTertiary,
            text = text,
            textSecondary = textSecondary,
            textButton = textButton,
            icon = icon,
            iconSecondary = iconSecondary,
            primaryButton = primaryButton,
            divider = divider,
            border = border,
            borderSecondary = borderSecondary,
            error = error,
            warning = warning,
            cardOnPrimary = cardOnPrimary,
            cardOnSecondary = cardOnSecondary
        )
    }
}

private val LightColorPalette = RadixColors(
    blue2 = Blue2,
    green1 = Green1,
    green3 = Green3,
    pink1 = Pink1,
    gray1 = Gray1,
    gray2 = Gray2,
    gray3 = Gray3,
    gray4 = Gray4,
    gray5 = Gray5,
    orange1 = Orange1,
    orange2 = Orange2,
    orange3 = Orange3,
    lightOrange = LightOrange,
    red1 = Red1,
    lightRed = LightRed,
    white = White,
    background = White,
    backgroundSecondary = Gray5,
    backgroundTertiary = Gray4,
    text = Gray1,
    textSecondary = Gray2,
    textButton = Blue2,
    icon = Gray1,
    iconSecondary = Gray2,
    primaryButton = Blue2,
    divider = Gray4,
    border = Gray1,
    borderSecondary = Gray3,
    error = Red1,
    warning = Orange3,
    cardOnPrimary = Gray4,
    cardOnSecondary = White
)

private val DarkColorPalette = LightColorPalette.copy(
    background = Color(0xFF28292A),
    backgroundSecondary = Color(0xFF1E1F1F),
    backgroundTertiary = Color(0xFF404243),
    text = Gray5,
    textSecondary = Gray4,
    textButton = Color(0xFF90CAF9),
    icon = Gray5,
    iconSecondary = Gray4,
    primaryButton = Color(0xFF00C389),
    divider = Color(0xFF404243),
    border = Gray5,
    borderSecondary = Gray3,
    error = Red1,
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
