package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import com.radixdlt.sargon.ResolvedReceiver

@Composable
fun Modifier.rnsGradient(
    receiver: ResolvedReceiver,
    shape: Shape
): Modifier {
    val brush = remember(receiver.domain) {
        val colors = listOf(
            Color(receiver.domain.gradientColorStart.toColorInt()),
            Color(receiver.domain.gradientColorEnd.toColorInt())
        )

        Brush.linearGradient(colors)
    }

    return this then Modifier
        .background(
            brush = brush,
            shape = shape
        )
}

sealed interface RNSPaletteState {
    data object Loading: RNSPaletteState

    data class Image(val palette: Palette): RNSPaletteState

    data object Error: RNSPaletteState
}