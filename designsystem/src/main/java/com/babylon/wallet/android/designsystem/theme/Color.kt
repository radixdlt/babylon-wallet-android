@file:Suppress("MagicNumber")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Blue1 = Color(0xFF060F8F)
val Blue2 = Color(0xFF052CC0)
val Blue3 = Color(0xFF20E4FF)

val Green1 = Color(0xFF00AB84)
val Green2 = Color(0xFF00C389)
val Green3 = Color(0xFF21FFBE)

val Pink1 = Color(0xFFCE0D98)
val Pink2 = Color(0xFFFF43CA)

val Gray1 = Color(0xFF003057)
val Gray2 = Color(0xFF8A8FA4)
val Gray3 = Color(0xFFCED0D6)
val Gray4 = Color(0xFFE2E5ED)
val Gray5 = Color(0xFFF4F5F9)

// alert
val Orange1 = Color(0xFFF2AD21)
val Red1 = Color(0xFFC82020)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

val DarkMode1 = Color(0xFF28292A)
val DarkMode2 = Color(0xFF404243)
val DarkMode3 = Color(0xFF373839)

val RadixGrey2 = Color(138, 143, 164)
val RadixBackground = Color(226, 226, 226)
val RadixCardBackground = Color(190, 189, 189)
val RadixLightCardBackground = Color(244, 244, 244)
val RadixButtonBackground = Color(83, 83, 83)

val GradientBrand1 = Brush.linearGradient(
    listOf(Color(0xFF03B797), Color(0xFF1544F5), Color(0xFFFF07E6), Color(0xFF060F8F)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(
        Float.POSITIVE_INFINITY, 0f
    )
)

val GradientBrand2 = Brush.linearGradient(
    listOf(Color(0xFF03B797), Color(0xFF1544F5), Color(0xFFFF07E6), Color(0xFF060F8F))
)

val GradientAccount1 = listOf(Color(0xFF052CC0), Color(0xFF01E2A0))
val GradientAccount2 = listOf(Color(0xFF052CC0), Color(0xFFFF43CA))
val GradientAccount3 = listOf(Color(0xFF052CC0), Color(0xFF20E4FF))
val GradientAccount4 = listOf(Color(0xFF00AB84), Color(0xFF052CC0))
val GradientAccount5 = listOf(Color(0xFFCE0D98), Color(0xFF052CC0))
val GradientAccount6 = listOf(Color(0xFF0DCAE4), Color(0xFF052CC0))
val GradientAccount7 = listOf(Color(0xFF003057), Color(0xFF03D497))
val GradientAccount8 = listOf(Color(0xFF003057), Color(0xFFF31DBE))
val GradientAccount9 = listOf(Color(0xFF052CC0), Color(0xFF003057))
val GradientAccount10 = listOf(Color(0xFF0BA97D), Color(0xFF1AF4B5))
val GradientAccount11 = listOf(Color(0xFF7E0D5F), Color(0xFFE225B3))
val GradientAccount12 = listOf(Color(0xFF040B72), Color(0xFF1F48E2))

val AccountGradientList =
    listOf(
        GradientAccount1,
        GradientAccount2,
        GradientAccount3,
        GradientAccount4,
        GradientAccount5,
        GradientAccount6,
        GradientAccount7,
        GradientAccount8,
        GradientAccount9,
        GradientAccount10,
        GradientAccount11,
        GradientAccount12,
    )
