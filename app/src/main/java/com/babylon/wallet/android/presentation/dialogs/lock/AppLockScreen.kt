package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.babylon.wallet.android.AppLockContent

@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel = hiltViewModel(),
    onUnlock: () -> Unit
) {
    AppLockContent(onUnlock = {
        viewModel.onUnlock()
        onUnlock()
    })
}
