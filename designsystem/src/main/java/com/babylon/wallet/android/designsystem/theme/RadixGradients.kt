@file:Suppress("MagicNumber", "CompositionLocalAllowlist")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.radixdlt.sargon.AppearanceId

data class RadixGradients(
    private val blue1: Color,
    private val blue2: Color,
    private val blue3: Color,
    private val blue4: Color,
    private val blue5: Color,
    private val blue6: Color,
    private val green1: Color,
    private val green2: Color,
    private val green3: Color,
    private val green4: Color,
    private val green5: Color,
    private val pink1: Color,
    private val pink2: Color,
    private val pink3: Color,
    private val pink4: Color,
    private val pink5: Color,
) {

    val accountGradients: List<List<Color>> = listOf(
        listOf(blue3, green4),
        listOf(blue3, pink5),
        listOf(blue3, blue6),
        listOf(green1, blue3),
        listOf(pink2, blue3),
        listOf(blue5, blue3),
        listOf(blue2, green3),
        listOf(blue2, pink3),
        listOf(blue3, blue2),
        listOf(green2, green5),
        listOf(pink1, pink4),
        listOf(blue1, blue4),
    )

    val slideToSignGradient: List<Color> = listOf(
        green3,
        blue3,
        pink3,
        blue3
    )
}

private val LightGradientsPalette = RadixGradients(
    blue1 = Color(0xFF040B72),
    blue2 = Color(0xFF003057),
    blue3 = Color(0xFF052CC0),
    blue4 = Color(0xFF1F48E2),
    blue5 = Color(0xFF0DCAE4),
    blue6 = Color(0xFF20E4FF),
    green1 = Color(0xFF00AB84),
    green2 = Color(0xFF0BA97D),
    green3 = Color(0xFF03D497),
    green4 = Color(0xFF01E2A0),
    green5 = Color(0xFF1AF4B5),
    pink1 = Color(0xFF7E0D5F),
    pink2 = Color(0xFFCE0D98),
    pink3 = Color(0xFFF31DBE),
    pink4 = Color(0xFFE225B3),
    pink5 = Color(0xFFFF28C2)
)

private val DarkGradientsPalette = RadixGradients(
    blue1 = Color(0xFF222772),
    blue2 = Color(0xFF1A3B56),
    blue3 = Color(0xFF3149A5),
    blue4 = Color(0xFF3C5AC9),
    blue5 = Color(0xFF3CB8C9),
    blue6 = Color(0xFF44D2E5),
    green1 = Color(0xFF2B917A),
    green2 = Color(0xFF2A8E72),
    green3 = Color(0xFF37BA94),
    green4 = Color(0xFF3CC9A0),
    green5 = Color(0xFF41DBAE),
    pink1 = Color(0xFF631D50),
    pink2 = Color(0xFFB53691),
    pink3 = Color(0xFFD841B3),
    pink4 = Color(0xFFC93CA6),
    pink5 = Color(0xFFE544B8)
)

internal val LocalRadixGradients = staticCompositionLocalOf<RadixGradients> {
    error("No RadixGradients provided")
}

@Composable
internal fun ProvideRadixGradients(content: @Composable () -> Unit) {
    val isDarkMode = LocalRadixThemeConfig.current.isDarkTheme
    val gradients = remember(isDarkMode) {
        if (isDarkMode) {
            DarkGradientsPalette
        } else {
            LightGradientsPalette
        }
    }
    CompositionLocalProvider(LocalRadixGradients provides gradients) {
        content()
    }
}

@Composable
internal fun Int.indexToGradient(alpha: Float = 1f): Brush {
    val gradients = RadixTheme.gradients.accountGradients
    val gradientsWithAlpha = remember(this, alpha) {
        gradients[this % gradients.size].map { it.copy(alpha = alpha) }
    }

    return Brush.horizontalGradient(gradientsWithAlpha)
}

@Composable
fun AppearanceId.gradient(alpha: Float = 1f): Brush {
    val index = remember(this) { value.toInt() }
    return index.indexToGradient(alpha = alpha)
}
