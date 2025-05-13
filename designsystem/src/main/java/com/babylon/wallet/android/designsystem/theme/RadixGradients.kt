package com.babylon.wallet.android.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.radixdlt.sargon.AppearanceId

////// Light gradients
private val GradientAccount1Light = listOf(
    Color(0xFF052CC0),
    Color(0xFF01E2A0)
)
private val GradientAccount2Light = listOf(
    Color(0xFF052CC0),
    Color(0xFFFF43CA)
)
private val GradientAccount3Light = listOf(
    Color(0xFF052CC0),
    Color(0xFF20E4FF)
)
private val GradientAccount4Light = listOf(
    Color(0xFF00AB84),
    Color(0xFF052CC0)
)
private val GradientAccount5Light = listOf(
    Color(0xFFCE0D98),
    Color(0xFF052CC0)
)
private val GradientAccount6Light = listOf(
    Color(0xFF0DCAE4),
    Color(0xFF052CC0)
)
private val GradientAccount7Light = listOf(
    Color(0xFF003057),
    Color(0xFF03D497)
)
private val GradientAccount8Light = listOf(
    Color(0xFF003057),
    Color(0xFFF31DBE)
)
private val GradientAccount9Light = listOf(
    Color(0xFF052CC0),
    Color(0xFF003057)
)
private val GradientAccount10Light = listOf(
    Color(0xFF0BA97D),
    Color(0xFF1AF4B5)
)
private val GradientAccount11Light = listOf(
    Color(0xFF7E0D5F),
    Color(0xFFE225B3)
)
private val GradientAccount12Light = listOf(
    Color(0xFF040B72),
    Color(0xFF1F48E2)
)
private val AccountGradientListLight = listOf(
    GradientAccount1Light,
    GradientAccount2Light,
    GradientAccount3Light,
    GradientAccount4Light,
    GradientAccount5Light,
    GradientAccount6Light,
    GradientAccount7Light,
    GradientAccount8Light,
    GradientAccount9Light,
    GradientAccount10Light,
    GradientAccount11Light,
    GradientAccount12Light,
)

////// Dark gradients
private val GradientAccount1Dark = listOf(
    Color(0xFF2D3A86),
    Color(0xFF2D866E)
)
private val GradientAccount2Dark = listOf(
    Color(0xFF2D3A86),
    Color(0xFF862D6D)
)
private val GradientAccount3Dark = listOf(
    Color(0xFF2D3A86),
    Color(0xFF2C7D87)
)
private val GradientAccount4Dark = listOf(
    Color(0xFF2E8F7D),
    Color(0xFF2D3A86)
)
private val GradientAccount5Dark = listOf(
    Color(0xFF862D6D),
    Color(0xFF2D3A86)
)
private val GradientAccount6Dark = listOf(
    Color(0xFF2D7B86),
    Color(0xFF2D3A86)
)
private val GradientAccount7Dark = listOf(
    Color(0xFF2D5E86),
    Color(0xFF2D866C)
)
private val GradientAccount8Dark = listOf(
    Color(0xFF2D5E86),
    Color(0xFF862D70)
)
private val GradientAccount9Dark = listOf(
    Color(0xFF2D3A86),
    Color(0xFF2D5E86)
)
private val GradientAccount10Dark = listOf(
    Color(0xFF2D866D),
    Color(0xFF2D866C)
)
private val GradientAccount11Dark = listOf(
    Color(0xFF862D6D),
    Color(0xFF862D70)
)
// TODO Theme
private val GradientAccount12Dark = listOf(
    Color(0xFF040B72),
    Color(0xFF1F48E2)
)
private val AccountGradientListDark = listOf(
    GradientAccount1Dark,
    GradientAccount2Dark,
    GradientAccount3Dark,
    GradientAccount4Dark,
    GradientAccount5Dark,
    GradientAccount6Dark,
    GradientAccount7Dark,
    GradientAccount8Dark,
    GradientAccount9Dark,
    GradientAccount10Dark,
    GradientAccount11Dark,
    GradientAccount12Dark,
)

val SlideToSignLight = Brush.linearGradient(
    listOf(
        Color(0xFF03B797),
        Color(0xFF052CC0),
        Color(0xFFFF07E6),
        Color(0xFF060F8F) // TODO Theme
    )
)
val SlideToSignDark = Brush.linearGradient(
    listOf(
        Color(0xFF2D8676),
        Color(0xFF2D3A86),
        Color(0xFF862D7D),
        Color(0xFF060F8F)
    )
)

@Composable
internal fun Int.indexToGradient(alpha: Float = 1f): Brush {
    val isDarkTheme = RadixTheme.config.isDarkTheme

    val colors = remember(this, alpha) {
        if (isDarkTheme) {
            AccountGradientListDark[this % AccountGradientListDark.size]
        } else {
            AccountGradientListLight[this % AccountGradientListLight.size]
        }.map { it.copy(alpha = alpha) }
    }

    return Brush.horizontalGradient(colors)
}

@Composable
fun AppearanceId.gradient(alpha: Float = 1f): Brush {
    val index = remember(this) { value.toInt() }
    return index.indexToGradient(alpha = alpha)
}
