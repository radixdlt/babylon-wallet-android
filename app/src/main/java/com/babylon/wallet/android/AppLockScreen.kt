package com.babylon.wallet.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.babylon.wallet.android.designsystem.SetStatusBarColor
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.babylon.wallet.android.utils.BiometricAuthenticationResult
import com.babylon.wallet.android.utils.biometricAuthenticate
import com.babylon.wallet.android.utils.findFragmentActivity

@Composable
fun AppLockScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SetStatusBarColor(useDarkIcons = false)
    BackHandler {
        context.findFragmentActivity()?.moveTaskToBack(true)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadixTheme.colors.blue1)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(288.dp)
                .align(Alignment.Center),
            tint = Color.Unspecified
        )
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            context.biometricAuthenticate {
                if (it == BiometricAuthenticationResult.Succeeded) {
                    onUnlock()
                } else {
                    context.findFragmentActivity()?.moveTaskToBack(true)
                }
            }
        }
    }
}
