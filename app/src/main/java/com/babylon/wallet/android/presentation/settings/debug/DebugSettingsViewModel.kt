package com.babylon.wallet.android.presentation.settings.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class DebugSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkConnectionStatusIndicatorState = preferencesManager
        .isLinkConnectionStatusIndicatorEnabled
        .mapLatest { isEnabled ->
            LinkConnectionStatusIndicator(isEnabled = isEnabled)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
            initialValue = LinkConnectionStatusIndicator(isEnabled = true)
        )

    fun onLinkConnectionStatusIndicatorToggled(isEnabled: Boolean) = viewModelScope.launch {
        preferencesManager.setLinkConnectionStatusIndicator(isEnabled = isEnabled)
    }

    data class LinkConnectionStatusIndicator(val isEnabled: Boolean)
}
