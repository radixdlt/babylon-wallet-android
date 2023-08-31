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
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.Profile
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getBackupStateUseCase: GetBackupStateUseCase,
    getImportOlympiaSettingVisibilityUseCase: GetImportOlympiaSettingVisibilityUseCase,
    private val markImportOlympiaWalletCompleteUseCase: MarkImportOlympiaWalletCompleteUseCase
) : ViewModel() {

    private val defaultSettings = if (EXPERIMENTAL_FEATURES_ENABLED) {
        persistentListOf(
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AccountSecurityAndSettings,
            SettingsItem.TopLevelSettings.AppSettings,
//            SettingsItem.TopLevelSettings.LinkedConnectors,
//            SettingsItem.TopLevelSettings.Gateways,
//            SettingsItem.TopLevelSettings.SeedPhrases,
//            SettingsItem.TopLevelSettings.LedgerHardwareWallets,
//            SettingsItem.TopLevelSettings.ImportFromLegacyWallet
        )
    } else {
        persistentListOf(
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AccountSecurityAndSettings,
            SettingsItem.TopLevelSettings.AppSettings,
//            SettingsItem.TopLevelSettings.LinkedConnectors,
//            SettingsItem.TopLevelSettings.Gateways,
//            SettingsItem.TopLevelSettings.SeedPhrases,
//            SettingsItem.TopLevelSettings.LedgerHardwareWallets
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        getProfileUseCase(),
        getBackupStateUseCase(),
        getImportOlympiaSettingVisibilityUseCase()
    ) { profile: Profile, backupState: BackupState, isImportFromOlympiaSettingDismissed ->
        val showConnectionSetting = profile.appPreferences.p2pLinks.isEmpty()

        // Update connection settings based on p2p links
        val visibleSettings = if (showConnectionSetting) {
            defaultSettings.toMutableList().apply {
                if (!contains(SettingsItem.TopLevelSettings.LinkToConnector)) {
                    add(0, SettingsItem.TopLevelSettings.LinkToConnector)
                }
            }.also {
                if (isImportFromOlympiaSettingDismissed.not()) {
                    it.apply {
                        if (!contains(SettingsItem.TopLevelSettings.ImportOlympiaWallet)) {
                            add(1, SettingsItem.TopLevelSettings.ImportOlympiaWallet)
                        }
                    }
                }
            }
        } else {
            if (isImportFromOlympiaSettingDismissed.not()) {
                defaultSettings.toMutableList().apply {
                    if (!contains(SettingsItem.TopLevelSettings.ImportOlympiaWallet)) {
                        add(0, SettingsItem.TopLevelSettings.ImportOlympiaWallet)
                    }
                }
            }
            defaultSettings.filter { settingSectionItem ->
                settingSectionItem != SettingsItem.TopLevelSettings.LinkToConnector
            }.toMutableList()
        }

//        if (visibleSettings.any { it is SettingsItem.TopLevelSettings.Backups }) {
//            visibleSettings.mapWhen(
//                predicate = { it is SettingsItem.TopLevelSettings.Backups },
//                mutation = {
//                    SettingsItem.TopLevelSettings.Backups(backupState)
//                }
//            )
//        } else {
//            visibleSettings.add(visibleSettings.lastIndex - 2, SettingsItem.TopLevelSettings.Backups(backupState))
//        }

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
