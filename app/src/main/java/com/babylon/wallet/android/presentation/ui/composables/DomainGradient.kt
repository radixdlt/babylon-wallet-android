package com.babylon.wallet.android.presentation.ui.composables

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.presentation.transfer.accounts.Domain
import com.babylon.wallet.android.presentation.ui.modifier.radixPlaceholder

@Composable
fun Modifier.rnsGradient(
    domain: Domain,
    defaultColor: Color,
    shape: Shape
): Modifier {
    val context = LocalContext.current
    val isDarkTheme = RadixTheme.config.isDarkTheme

    val paletteState by produceState<RNSPaletteState>(RNSPaletteState.Loading, domain.imageUrl) {
        val request = ImageRequest.Builder(context)
            .data(domain.imageUrl)
            .build()

        val res = ImageLoader(context).execute(
            request = request
        )

        val bitmap = res.drawable?.toBitmapOrNull()
        if (bitmap == null) {
            value = RNSPaletteState.Error
            return@produceState;
        }

        val copied = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        value = RNSPaletteState.Image(Palette.from(copied).generate())
    }

    val brush by produceState<Brush>(
        SolidColor(defaultColor),
        paletteState,
        isDarkTheme
    ) {
        val palette = (paletteState as? RNSPaletteState.Image)?.palette

        if (palette == null) {
            value = SolidColor(defaultColor)
            return@produceState
        }

        val colors = listOfNotNull(
            palette.darkMutedSwatch,
            palette.mutedSwatch,
            palette.lightMutedSwatch,
            palette.darkVibrantSwatch,
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
        ).map {
            val argb = ColorUtils.setAlphaComponent(
                it.rgb,
                255
            )
            Color(argb)
        }

        value = if (colors.isEmpty()) {
            SolidColor(defaultColor)
        } else if (colors.size == 1) {
            SolidColor(colors.first())
        } else {
            Brush.linearGradient(colors)
        }
    }

    return this then Modifier
        .radixPlaceholder(
            visible = paletteState is RNSPaletteState.Loading,
            shape = shape
        )
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