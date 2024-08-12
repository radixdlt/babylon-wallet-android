package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.appLockScreen() {
    navigate(route = "app_lock")
}

fun NavGraphBuilder.appLock(
    onUnlock: () -> Unit
) {
    dialog(
        route = "app_lock",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        AppLockDialog(onUnlock = onUnlock)
    }
}
