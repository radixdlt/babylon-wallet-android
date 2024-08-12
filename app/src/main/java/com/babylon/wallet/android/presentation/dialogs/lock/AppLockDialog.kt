package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.AppLockScreen

@Composable
fun AppLockDialog(
    viewModel: AppLockViewModel = hiltViewModel(),
    onUnlock: () -> Unit
) {
    AppLockScreen(onUnlock = {
        viewModel.onUnlock()
        onUnlock()
    })
}