package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

inline fun Modifier.throttleClickable(
    thresholdMs: Long = 500L,
    crossinline onClick: () -> Unit
): Modifier {
    return composed {
        var lastClickMs by remember { mutableStateOf(0L) }
        clickable {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > thresholdMs) {
                onClick()
                lastClickMs = now
            }
        }
    }
}

inline fun Modifier.throttleClickableNoIndicator(
    thresholdMs: Long = 500L,
    crossinline onClick: () -> Unit
): Modifier {
    return composed {
        var lastClickMs by remember { mutableStateOf(0L) }
        clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > thresholdMs) {
                onClick()
                lastClickMs = now
            }
        }
    }
}
