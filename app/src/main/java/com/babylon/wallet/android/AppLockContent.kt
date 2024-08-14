package com.babylon.wallet.android

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.babylon.wallet.android.presentation.ui.composables.LockScreenBackground
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun AppLockContent(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BackHandler {
        context.findFragmentActivity()?.moveTaskToBack(true)
    }
    LockScreenBackground(modifier = modifier, onTapToUnlock = {
        context.biometricAuthenticate {
            if (it == BiometricAuthenticationResult.Succeeded) {
                onUnlock()
            }
        }
    })
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            context.biometricAuthenticate {
                if (it == BiometricAuthenticationResult.Succeeded) {
                    onUnlock()
                }
            }
        }
    }
}
