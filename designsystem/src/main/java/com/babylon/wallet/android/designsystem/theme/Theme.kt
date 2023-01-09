@file:Suppress("MagicNumber", "LongParameterList", "CompositionLocalAllowlist")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babylon.wallet.android.designsystem.SetStatusBarColor

// TODO - IMPORTANT
//  The resource-theme of the app has been switched from Material 2 to Material 3.
//  The reason was to fix the bug that caused a crash with the biometric dialog on Android 6 to 8.1.
//  Although the resource-theme is set to Material 3,
//  the rest of the UI components and the UI theme is still in Material 2.
//  Once we have a design system we should update the Theme file following the Material Design 3 guidelines
//  and switch the UI components from Material 2 to 3 if it is needed!
//  For samples have a look at Jetsurvey and JetNews

@Stable
class RadixColors(
    defaultBackground: Color,
    backgroundAlternate: Color,
    defaultText: Color,
    blue1: Color,
    blue2: Color,
    blue3: Color,
    green1: Color,
    green2: Color,
    green3: Color,
    pink1: Color,
    pink2: Color,
    gray1: Color,
    gray2: Color,
    gray3: Color,
    gray4: Color,
    gray5: Color,
    orange1: Color,
    orange2: Color,
    red1: Color,
    white: Color,
    darkMode1: Color,
    darkMode2: Color,
    darkMode3: Color,
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
    var blue3 by mutableStateOf(blue3)
        private set
    var green1 by mutableStateOf(green1)
        private set
    var green2 by mutableStateOf(green2)
        private set
    var green3 by mutableStateOf(green3)
        private set
    var pink1 by mutableStateOf(pink1)
        private set
    var pink2 by mutableStateOf(pink2)
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
    var red1 by mutableStateOf(red1)
        private set
    var white by mutableStateOf(white)
        private set
    var darkMode1 by mutableStateOf(darkMode1)
        private set
    var darkMode2 by mutableStateOf(darkMode2)
        private set
    var darkMode3 by mutableStateOf(darkMode3)
        private set

    fun copy(
        defaultBackground: Color = this.defaultBackground,
        backgroundAlternate: Color = this.backgroundAlternate,
        defaultText: Color = this.defaultText,
        blue1: Color = this.blue1,
        blue2: Color = this.blue2,
        blue3: Color = this.blue3,
        green1: Color = this.green1,
        green2: Color = this.green2,
        green3: Color = this.green3,
        pink1: Color = this.pink1,
        pink2: Color = this.pink2,
        gray1: Color = this.gray1,
        gray2: Color = this.gray2,
        gray3: Color = this.gray3,
        gray4: Color = this.gray4,
        gray5: Color = this.gray5,
        orange1: Color = this.orange1,
        orange2: Color = this.orange2,
        red1: Color = this.red1,
        white: Color = this.white,
        darkMode1: Color = this.darkMode1,
        darkMode2: Color = this.darkMode2,
        darkMode3: Color = this.darkMode3,
    ): RadixColors {
        return RadixColors(
            defaultBackground,
            backgroundAlternate,
            defaultText,
            blue1,
            blue2,
            blue3,
            green1,
            green2,
            green3,
            pink1,
            pink2,
            gray1,
            gray2,
            gray3,
            gray4,
            gray5,
            orange1,
            orange2,
            red1,
            white,
            darkMode1,
            darkMode2,
            darkMode3
        )
    }

    fun update(other: RadixColors) {
        defaultBackground = other.defaultBackground
        defaultText = other.defaultText
        blue1 = other.blue1
        blue2 = other.blue2
        blue3 = other.blue3
        green1 = other.green1
        green2 = other.green2
        green3 = other.green3
        pink1 = other.pink1
        pink2 = other.pink2
        gray1 = other.gray1
        gray2 = other.gray2
        gray3 = other.gray3
        gray4 = other.gray4
        gray5 = other.gray5
        orange1 = other.orange1
        orange2 = other.orange2
        red1 = other.red1
        white = other.white
        darkMode1 = other.darkMode1
        darkMode2 = other.darkMode2
        darkMode3 = other.darkMode3
    }
}

private val LightColorPalette = RadixColors(
    defaultBackground = White,
    backgroundAlternate = Black,
    defaultText = Black,
    blue1 = Blue1,
    blue2 = Blue2,
    blue3 = Blue3,
    green1 = Green1,
    green2 = Green2,
    green3 = Green3,
    pink1 = Pink1,
    pink2 = Pink2,
    gray1 = Gray1,
    gray2 = Gray2,
    gray3 = Gray3,
    gray4 = Gray4,
    gray5 = Gray5,
    orange1 = Orange1,
    orange2 = Orange2,
    red1 = Red1,
    white = White,
    darkMode1 = DarkMode1,
    darkMode2 = DarkMode2,
    darkMode3 = DarkMode3,
)

@Suppress("UnusedPrivateMember")
private val DarkColorPalette = RadixColors(
    defaultBackground = Black,
    backgroundAlternate = White,
    defaultText = White,
    blue1 = Blue1,
    blue2 = Blue2,
    blue3 = Blue3,
    green1 = Green1,
    green2 = Green2,
    green3 = Green3,
    pink1 = Pink1,
    pink2 = Pink2,
    gray1 = Gray1,
    gray2 = Gray2,
    gray3 = Gray3,
    gray4 = Gray4,
    gray5 = Gray5,
    orange1 = Orange1,
    orange2 = Orange2,
    red1 = Red1,
    white = White,
    darkMode1 = DarkMode1,
    darkMode2 = DarkMode2,
    darkMode3 = DarkMode3,
)

private val MaterialLightColorPalette = lightColors(
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
)

private val LocalRadixColors = staticCompositionLocalOf<RadixColors> {
    error("No RadixColors provided")
}

@Composable
fun ProvideRadixColors(colors: RadixColors, content: @Composable () -> Unit) {
    val colorPalette = remember {
        colors.copy()
    }
    colorPalette.update(colors)
    CompositionLocalProvider(LocalRadixColors provides colorPalette) {
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
fun ProvideRadixThemeConfig(isDarkMode: Boolean, content: @Composable () -> Unit) {
    val themeConfig = remember {
        RadixThemeConfig(
            isDarkTheme = isDarkMode,
        )
    }
    CompositionLocalProvider(LocalRadixThemeConfig provides themeConfig) {
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        // TODO update to dark color palette when we have it ready
        LightColorPalette
    } else {
        LightColorPalette
    }
    ProvideRadixThemeConfig(isDarkMode = darkTheme) {
        ProvideRadixColors(colors) {
            // TODO status bar color when we remove dev banner
//            SetStatusBarColor(color = colors.defaultBackground, useDarkIcons = !darkTheme)
            SetStatusBarColor(color = colors.orange1, useDarkIcons = !darkTheme)
            ProvideRadixTypography {
                ProvideRadixDimensions {
                    ProvideRadixShapes {
                        MaterialTheme(
                            colors = MaterialLightColorPalette,
                            typography = DefaultTypography,
                            shapes = debugShapes()
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

fun debugColors(
    darkTheme: Boolean,
    debugColor: Color = Color.Magenta,
) = Colors(
    primary = debugColor,
    primaryVariant = debugColor,
    secondary = debugColor,
    secondaryVariant = debugColor,
    background = debugColor,
    surface = debugColor,
    error = debugColor,
    onPrimary = debugColor,
    onSecondary = debugColor,
    onBackground = debugColor,
    onSurface = debugColor,
    onError = debugColor,
    isLight = !darkTheme
)

fun debugShapes() = Shapes(
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp)
)

fun debugTypography() = Typography(
    body1 = TextStyle(fontSize = 4.sp),
    body2 = TextStyle(fontSize = 4.sp),
    button = TextStyle(fontSize = 4.sp),
    caption = TextStyle(fontSize = 4.sp),
    h1 = TextStyle(fontSize = 4.sp),
    h2 = TextStyle(fontSize = 4.sp),
    h3 = TextStyle(fontSize = 4.sp),
    h4 = TextStyle(fontSize = 4.sp),
    h5 = TextStyle(fontSize = 4.sp),
    h6 = TextStyle(fontSize = 4.sp),
    overline = TextStyle(fontSize = 4.sp),
    subtitle1 = TextStyle(fontSize = 4.sp),
    subtitle2 = TextStyle(fontSize = 4.sp),
)
