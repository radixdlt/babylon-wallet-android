package com.babylon.wallet.android.designsystem.utils

import androidx.compose.runtime.MutableState

object ClickListenerUtils {

    const val CLICK_THROTTLE_MS = 500L

    fun throttleOnClick(
        lastClickMs: MutableState<Long>,
        onClick: () -> Unit,
        throttleMs: Long = CLICK_THROTTLE_MS,
        enabled: Boolean = true
    ) {
        if (enabled) {
            val now = System.currentTimeMillis()
            if (now - lastClickMs.value > throttleMs) {
                lastClickMs.value = now
                onClick()
            }
        } else {
            onClick()
        }
    }
}
