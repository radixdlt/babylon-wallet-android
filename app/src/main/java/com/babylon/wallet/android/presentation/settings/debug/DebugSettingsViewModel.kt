package com.babylon.wallet.android.presentation.settings.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class DebugSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val state = combine(
        preferencesManager.isLinkConnectionStatusIndicatorEnabled,
        preferencesManager.isAppLockEnabled
    ) { isLinkConnectionStatusIndicatorEnabled, isAppLockEnabled ->
        State(
            isLinkConnectionStatusIndicatorEnabled = isLinkConnectionStatusIndicatorEnabled,
            isAppLockEnabled = isAppLockEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        initialValue = State(
            isLinkConnectionStatusIndicatorEnabled = true,
            isAppLockEnabled = true
        )
    )

    fun onLinkConnectionStatusIndicatorToggled(isEnabled: Boolean) = viewModelScope.launch {
        preferencesManager.setLinkConnectionStatusIndicator(isEnabled = isEnabled)
    }

    fun onToggleAppLock(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.enableAppLock(enabled)
        }
    }

    data class State(
        val isLinkConnectionStatusIndicatorEnabled: Boolean,
        val isAppLockEnabled: Boolean
    ) : UiState
}
