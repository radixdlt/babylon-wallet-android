package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.domain.usecases.settings.GetImportOlympiaSettingVisibilityUseCase
import com.babylon.wallet.android.domain.usecases.settings.MarkImportOlympiaWalletCompleteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Profile
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getImportOlympiaSettingVisibilityUseCase: GetImportOlympiaSettingVisibilityUseCase,
    private val markImportOlympiaWalletCompleteUseCase: MarkImportOlympiaWalletCompleteUseCase
) : ViewModel() {

    private val defaultSettings = if (EXPERIMENTAL_FEATURES_ENABLED) {
        persistentListOf(
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AccountSecurityAndSettings,
            SettingsItem.TopLevelSettings.AppSettings
        )
    } else {
        persistentListOf(
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AccountSecurityAndSettings,
            SettingsItem.TopLevelSettings.AppSettings
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        getProfileUseCase(),
        getImportOlympiaSettingVisibilityUseCase()
    ) { profile: Profile, isImportFromOlympiaSettingDismissed ->
        val showConnectionSetting = profile.appPreferences.p2pLinks.isEmpty()

        // Update connection settings based on p2p links
        val visibleSettings = if (showConnectionSetting) {
            defaultSettings.toMutableList().apply {
                if (!contains(SettingsItem.TopLevelSettings.LinkToConnector)) {
                    add(0, SettingsItem.TopLevelSettings.LinkToConnector)
                }
                if (isImportFromOlympiaSettingDismissed.not()) {
                    if (!contains(SettingsItem.TopLevelSettings.ImportOlympiaWallet)) {
                        add(1, SettingsItem.TopLevelSettings.ImportOlympiaWallet)
                    }
                }
            }
        } else {
            defaultSettings.toMutableList().apply {
                if (isImportFromOlympiaSettingDismissed.not()) {
                    add(0, SettingsItem.TopLevelSettings.ImportOlympiaWallet)
                }
            }
        }
        SettingsUiState(visibleSettings.toPersistentList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings)
    )

    fun hideImportOlympiaWalletSettingBox() {
        viewModelScope.launch {
            markImportOlympiaWalletCompleteUseCase()
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.TopLevelSettings>
)
