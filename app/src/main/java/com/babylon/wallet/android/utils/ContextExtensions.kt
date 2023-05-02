package com.babylon.wallet.android.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.fragment.app.FragmentActivity

fun Context.biometricAuthenticate(authenticationCallback: (successful: Boolean) -> Unit) {
    findFragmentActivity()?.let { activity ->
        activity.biometricAuthenticate(true) { authenticatedSuccessfully ->
            authenticationCallback(authenticatedSuccessfully)
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is FragmentActivity) return context
        context = context.baseContext
    }
    return null
}

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
