@file:Suppress("MagicNumber", "LongParameterList", "CompositionLocalAllowlist")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

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

internal val LocalRadixThemeConfig = staticCompositionLocalOf<RadixThemeConfig> {
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
