package com.babylon.wallet.android.presentation.dialogs.lock

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.babylon.wallet.android.AppLockContent
import com.babylon.wallet.android.presentation.ui.composables.NotSecureAlertDialog
import com.babylon.wallet.android.utils.findFragmentActivity

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
