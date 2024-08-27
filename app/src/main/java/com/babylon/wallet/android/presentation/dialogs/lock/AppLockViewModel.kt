package com.babylon.wallet.android.presentation.dialogs.lock

import com.babylon.wallet.android.AppLockStateProvider
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.DeviceCapabilityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockStateProvider: AppLockStateProvider,
    private val deviceCapabilityHelper: DeviceCapabilityHelper,
) : StateViewModel<AppLockViewModel.State>() {

    fun onUnlock() {
        appLockStateProvider.unlockApp()
    }

    data class State(
        val isDeviceSecure: Boolean
    ) : UiState

    fun onResumed() {
        _state.update {
            it.copy(
                isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
            )
        }
    }

    override fun initialState(): State {
        return State(
            isDeviceSecure = deviceCapabilityHelper.isDeviceSecure
        )
    }
}
