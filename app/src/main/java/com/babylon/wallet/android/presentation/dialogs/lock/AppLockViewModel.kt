package com.babylon.wallet.android.presentation.dialogs.lock

import androidx.lifecycle.ViewModel
import com.babylon.wallet.android.AppStateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appStateProvider: AppStateProvider
) : ViewModel() {

    fun onUnlock() {
        appStateProvider.unlockApp()
    }

}
