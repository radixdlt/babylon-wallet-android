package com.babylon.wallet.android.presentation.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.utils.setWindowSecure

@Composable
fun SecureScreen() {
    if (BuildConfig.DEBUG_MODE) {
        return
    } else {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            context.setWindowSecure(true)
            onDispose {
                context.setWindowSecure(false)
            }
        }
    }
}
