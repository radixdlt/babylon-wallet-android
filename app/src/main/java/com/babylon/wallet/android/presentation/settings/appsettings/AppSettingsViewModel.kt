package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import rdx.works.profile.domain.security.UpdateDeveloperModeUseCase
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getBackupStateUseCase: GetBackupStateUseCase,
    private val preferencesManager: PreferencesManager,
    private val updateDeveloperModeUseCase: UpdateDeveloperModeUseCase
) : StateViewModel<AppSettingsUiState>() {

    override fun initialState(): AppSettingsUiState = AppSettingsUiState.default

    init {
        readSettings()
    }

    private fun readSettings() {
        viewModelScope.launch {
            getProfileUseCase
                .security
                .map { it.isDeveloperModeEnabled }
                .collect { isInDeveloperMode ->
                    _state.updateSetting<SettingsItem.AppSettingsItem.DeveloperMode> {
                        SettingsItem.AppSettingsItem.DeveloperMode(isInDeveloperMode)
                    }
                }
        }
        if (BuildConfig.CRASH_REPORTING_AVAILABLE) {
            _state.update { settingsUiState ->
                settingsUiState.copy(
                    settings = (settingsUiState.settings + SettingsItem.AppSettingsItem.CrashReporting(false)).toPersistentSet()
                )
            }
            viewModelScope.launch {
                preferencesManager.isCrashReportingEnabled.collect { enabled ->
                    if (enabled) {
                        Firebase.crashlytics.deleteUnsentReports()
                    }
                    Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
                    _state.updateSetting<SettingsItem.AppSettingsItem.CrashReporting> {
                        SettingsItem.AppSettingsItem.CrashReporting(enabled)
                    }
                }
            }
        }
        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                _state.update { settingsUiState ->
                    val settingsList = if (settingsUiState.settings.any { it is SettingsItem.AppSettingsItem.Backups }) {
                        settingsUiState.settings.mapWhen(
                            predicate = { it is SettingsItem.AppSettingsItem.Backups },
                            mutation = { SettingsItem.AppSettingsItem.Backups(backupState = backupState) }
                        )
                    } else {
                        settingsUiState.settings.toMutableList().apply {
                            add(2, SettingsItem.AppSettingsItem.Backups(backupState))
                        }
                    }
                    settingsUiState.copy(settings = settingsList.toPersistentSet())
                }
            }
        }
    }

    fun onDeveloperModeToggled(enabled: Boolean) = viewModelScope.launch {
        updateDeveloperModeUseCase(isEnabled = enabled)
    }

    fun onCrashReportingToggled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.enableCrashReporting(enabled)
    }

    private inline fun <reified S : SettingsItem.AppSettingsItem> MutableStateFlow<AppSettingsUiState>.updateSetting(
        mutation: (S) -> S
    ) = update { uiState ->
        uiState.copy(
            settings = uiState.settings.mapWhen(
                predicate = { it is S },
                mutation = { mutation(it as S) }
            ).toPersistentSet()
        )
    }
}

data class AppSettingsUiState(
    val settings: ImmutableSet<SettingsItem.AppSettingsItem>
) : UiState {

    companion object {
        val default = AppSettingsUiState(
            settings = persistentSetOf(
                SettingsItem.AppSettingsItem.LinkedConnectors,
                SettingsItem.AppSettingsItem.Gateways,
                SettingsItem.AppSettingsItem.EntityHiding,
                SettingsItem.AppSettingsItem.DeveloperMode(false)
            )
        )
    }
}
