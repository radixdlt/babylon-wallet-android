package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.designsystem.theme.RadixTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

fun NavController.appLockScreen() {
    navigate(route = "app_lock")
}

fun NavGraphBuilder.appLock(
    onUnlock: () -> Unit
) {
    dialog(
        route = "app_lock",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val systemUiController = rememberSystemUiController()
        systemUiController.setStatusBarColor(RadixTheme.colors.blue1)
        systemUiController.setNavigationBarColor(RadixTheme.colors.blue1)
        AppLockDialog(onUnlock = onUnlock)
    }
}
