@file:Suppress("MagicNumber")

package com.babylon.wallet.android.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

val GradientBrand1 = Brush.linearGradient(
    listOf(Color(0xFF03B797), Color(0xFF1544F5), Color(0xFFFF07E6), Color(0xFF060F8F)),
    start = Offset(0f, Float.POSITIVE_INFINITY),
    end = Offset(
        x = Float.POSITIVE_INFINITY,
        y = 0f
    )
)

val GradientBrand2 = Brush.linearGradient(
    listOf(Color(0xFF03B797), Color(0xFF1544F5), Color(0xFFFF07E6), Color(0xFF060F8F))
)


