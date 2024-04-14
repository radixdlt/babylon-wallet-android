package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.deleteCrashlyticsUnsentReports
import rdx.works.core.enableCrashlytics
import rdx.works.core.mapWhen
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.security
import rdx.works.profile.domain.security.UpdateDeveloperModeUseCase
import javax.inject.Inject

@HiltViewModel
class WalletPreferencesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val updateDeveloperModeUseCase: UpdateDeveloperModeUseCase
) : StateViewModel<WalletPreferencesUiState>() {

    override fun initialState(): WalletPreferencesUiState = WalletPreferencesUiState.default

    init {
        readSettings()
    }

    private fun readSettings() {
        viewModelScope.launch {
            getProfileUseCase
                .security
                .map { it.isDeveloperModeEnabled }
                .collect { isInDeveloperMode ->
                    _state.updateSetting<SettingsItem.WalletPreferencesSettingsItem.DeveloperMode> {
                        SettingsItem.WalletPreferencesSettingsItem.DeveloperMode(isInDeveloperMode)
                    }
                }
        }
        if (BuildConfig.CRASH_REPORTING_AVAILABLE) {
            _state.update { settingsUiState ->
                val updateCrashReportingPreference = PreferencesUiItem.Preference(
                    SettingsItem.WalletPreferencesSettingsItem.CrashReporting(
                        false
                    )
                )
                settingsUiState.copy(
                    settings = (settingsUiState.settings + updateCrashReportingPreference).toPersistentSet()
                )
            }
            viewModelScope.launch {
                preferencesManager.isCrashReportingEnabled.collect { enabled ->
                    if (enabled) {
                        deleteCrashlyticsUnsentReports()
                    }
                    enableCrashlytics(enabled)
                    _state.updateSetting<SettingsItem.WalletPreferencesSettingsItem.CrashReporting> {
                        SettingsItem.WalletPreferencesSettingsItem.CrashReporting(enabled)
                    }
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

    private inline fun <reified S : SettingsItem.WalletPreferencesSettingsItem> MutableStateFlow<WalletPreferencesUiState>.updateSetting(
        mutation: (S) -> S
    ) = update { uiState ->
        uiState.copy(
            settings = uiState.settings.mapWhen(
                predicate = { it is PreferencesUiItem.Preference && it.item is S },
                mutation = {
                    it as PreferencesUiItem.Preference
                    PreferencesUiItem.Preference(mutation(it.item as S))
                }
            ).toPersistentSet()
        )
    }
}

sealed interface PreferencesUiItem {
    data object AdvancedSection : PreferencesUiItem
    data class Preference(val item: SettingsItem.WalletPreferencesSettingsItem) : PreferencesUiItem
}

data class WalletPreferencesUiState(
    val settings: ImmutableSet<PreferencesUiItem>
) : UiState {

    companion object {
        val default = WalletPreferencesUiState(
            settings = persistentSetOf(
                PreferencesUiItem.Preference(SettingsItem.WalletPreferencesSettingsItem.DepositGuarantees),
                PreferencesUiItem.Preference(SettingsItem.WalletPreferencesSettingsItem.EntityHiding),
                PreferencesUiItem.AdvancedSection,
                PreferencesUiItem.Preference(SettingsItem.WalletPreferencesSettingsItem.Gateways),
                PreferencesUiItem.Preference(SettingsItem.WalletPreferencesSettingsItem.DeveloperMode(false))
            )
        )
    }
}
