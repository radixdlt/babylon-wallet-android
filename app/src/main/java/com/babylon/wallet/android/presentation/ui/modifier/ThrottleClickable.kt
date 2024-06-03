package com.babylon.wallet.android.presentation.ui.modifier

import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.babylon.wallet.android.designsystem.utils.ClickListenerUtils

fun Modifier.throttleClickable(
    thresholdMs: Long = 500L,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    return composed {
        val lastClickMs = remember { mutableLongStateOf(0L) }
        clickable(enabled = enabled) {
            ClickListenerUtils.throttleOnClick(
                lastClickMs = lastClickMs,
                onClick = onClick,
                throttleMs = thresholdMs
            )
        }
    }
}
