package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val defaultSettings: ImmutableList<SettingsItem.AppSettings> = persistentListOf(
        SettingsItem.AppSettings.DeveloperMode(false)
    )

    val state = preferencesManager.developerMode.map { developerMode ->
        val updatedSettings = defaultSettings.toMutableList().apply {
            val index = indexOfFirst { it is SettingsItem.AppSettings.DeveloperMode }
            if (index != -1) {
                removeAt(index)
                add(index, SettingsItem.AppSettings.DeveloperMode(developerMode))
            }
        }.toPersistentList()
        SettingsUiState(updatedSettings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings)
    )

    fun onDeveloperModeToggled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDeveloperMode(enabled)
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.AppSettings>
)
