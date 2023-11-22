package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

inline fun Modifier.throttleClickable(
    thresholdMs: Long = 500L,
    enabled: Boolean = true,
    crossinline onClick: () -> Unit
): Modifier {
    return composed {
        var lastClickMs by remember { mutableStateOf(0L) }
        clickable(enabled = enabled) {
            val now = System.currentTimeMillis()
            if (now - lastClickMs > thresholdMs) {
                onClick()
                lastClickMs = now
            }
        }
    }
}
