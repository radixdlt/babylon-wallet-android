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
    defaultBackground: Color,
    backgroundAlternate: Color,
    defaultText: Color,
    blue1: Color,
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
    text: Color
) {
    var defaultBackground by mutableStateOf(defaultBackground)
        private set
    var backgroundAlternate by mutableStateOf(backgroundAlternate)
        private set
    var defaultText by mutableStateOf(defaultText)
        private set
    var blue1 by mutableStateOf(blue1)
        private set
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
    var text by mutableStateOf(text)
        private set


    fun copy(
        defaultBackground: Color = this.defaultBackground,
        backgroundAlternate: Color = this.backgroundAlternate,
        defaultText: Color = this.defaultText,
        blue1: Color = this.blue1,
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
        text: Color = this.text
    ): RadixColors {
        return RadixColors(
            defaultBackground = defaultBackground,
            backgroundAlternate = backgroundAlternate,
            defaultText = defaultText,
            blue1 = blue1,
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
            text = text
        )
    }

    fun update(other: RadixColors) {
        defaultBackground = other.defaultBackground
        defaultText = other.defaultText
        blue1 = other.blue1
        blue2 = other.blue2
        green1 = other.green1
        green3 = other.green3
        pink1 = other.pink1
        gray1 = other.gray1
        gray2 = other.gray2
        gray3 = other.gray3
        gray4 = other.gray4
        gray5 = other.gray5
        orange1 = other.orange1
        orange2 = other.orange2
        lightOrange = other.lightOrange
        red1 = other.red1
        lightRed = other.lightRed
        white = other.white
        background = other.background
        backgroundSecondary = other.backgroundSecondary
        text = other.text
    }
}

private val LightColorPalette = RadixColors(
    defaultBackground = White,
    backgroundAlternate = Black,
    defaultText = Black,
    blue1 = Blue1,
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
    text = Gray1
)

private val DarkColorPalette = LightColorPalette.copy(
    background = Color(0xFF28292A),
    backgroundSecondary = Color(0xFF1E1F1F),
    text = White
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
