package com.babylon.wallet.android

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.babylon.wallet.android.presentation.ui.composables.LockScreenBackground
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.coroutines.delay

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
    // We want this code to execute once. We can't use lifecycle event on older devices (API 28 for example)
    // when using PIN/password/pattern lock,
    // biometric prompt is new activity which covers current screen and trigger lifecycle methods
    LaunchedEffect(Unit) {
        delay(Constants.DELAY_300_MS)
        context.biometricAuthenticate {
            if (it == BiometricAuthenticationResult.Succeeded) {
                onUnlock()
            }
        }
    }
}
