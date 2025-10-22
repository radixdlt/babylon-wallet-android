package com.babylon.wallet.android.presentation.lockscreen

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.presentation.ui.composables.LockScreenBackground
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.Constants
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel = hiltViewModel(),
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isDeviceSecure.not()) {
        NotSecureAlertDialog(finish = {
            if (it) {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                ContextCompat.startActivity(context, intent, null)
            } else {
                context.findFragmentActivity()?.moveTaskToBack(true)
            }
        })
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.onResumed()
    }

    AppLockContent(onUnlock = {
        viewModel.onUnlock()
        onUnlock()
    })
}

@Composable
private fun AppLockContent(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit
) {
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
