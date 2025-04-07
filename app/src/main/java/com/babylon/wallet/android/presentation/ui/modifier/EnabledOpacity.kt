package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.enabledOpacity(
    isEnabled: Boolean,
    disabledOpacity: Float = 0.5f
) = if (isEnabled) {
    this
} else {
    this.then(
        Modifier.graphicsLayer(alpha = disabledOpacity)
    )
}
