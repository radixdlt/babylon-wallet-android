package com.babylon.wallet.android.presentation.ui.composables

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun SecureScreen() {
    if (BuildConfig.DEBUG_MODE) {
        return
    } else {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val window = context.findFragmentActivity()?.window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
