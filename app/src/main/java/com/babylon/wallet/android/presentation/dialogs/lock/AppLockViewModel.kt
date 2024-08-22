package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.AppLockStateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockStateProvider: AppLockStateProvider
) : ViewModel() {

    fun onUnlock() {
        appLockStateProvider.unlockApp()
    }
}
