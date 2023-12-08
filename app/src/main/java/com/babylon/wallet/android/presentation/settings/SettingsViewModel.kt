package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.domain.usecases.GetEntitiesWithSecurityPromptUseCase
import com.babylon.wallet.android.domain.usecases.settings.GetImportOlympiaSettingVisibilityUseCase
import com.babylon.wallet.android.domain.usecases.settings.MarkImportOlympiaWalletCompleteUseCase
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AccountSecurityAndSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AppSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AuthorizedDapps
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.DebugSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.ImportOlympiaWallet
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.LinkToConnector
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Personas
import com.babylon.wallet.android.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getImportOlympiaSettingVisibilityUseCase: GetImportOlympiaSettingVisibilityUseCase,
    private val markImportOlympiaWalletCompleteUseCase: MarkImportOlympiaWalletCompleteUseCase,
    getBackupStateUseCase: GetBackupStateUseCase,
    getEntitiesWithSecurityPromptUseCase: GetEntitiesWithSecurityPromptUseCase
) : ViewModel() {

    private val defaultSettings = listOf(
        AuthorizedDapps,
        Personas(),
        AccountSecurityAndSettings(showNotificationWarning = false),
        AppSettings,
        if (EXPERIMENTAL_FEATURES_ENABLED) DebugSettings else null
    ).mapNotNull { it }

    val state: StateFlow<SettingsUiState> = combine(
        getProfileUseCase(),
        getImportOlympiaSettingVisibilityUseCase(),
        getBackupStateUseCase(),
        getEntitiesWithSecurityPromptUseCase.shouldShowPersonaSecurityPrompt
    ) { profile: Profile, isImportFromOlympiaSettingDismissed: Boolean, backupState: BackupState, showPersonaPrompt: Boolean ->
        val mutated = defaultSettings.toMutableList()
        var topIndex = 0
        if (profile.appPreferences.p2pLinks.isEmpty() && !defaultSettings.contains(LinkToConnector)) {
            mutated.add(topIndex, LinkToConnector)
            topIndex += 1
        }

        val isImportFeatureAvailable = EXPERIMENTAL_FEATURES_ENABLED || profile.currentNetwork.networkID == Radix.Network.mainnet.id
        if (!isImportFromOlympiaSettingDismissed && !defaultSettings.contains(ImportOlympiaWallet) && isImportFeatureAvailable) {
            mutated.add(topIndex, ImportOlympiaWallet)
        }

        val withBackupWarning = mutated.mapWhen(predicate = { it is AccountSecurityAndSettings }) {
            AccountSecurityAndSettings(showNotificationWarning = backupState.isWarningVisible)
        }

        val withPersonaWarning = withBackupWarning.mapWhen(predicate = { it is Personas }) {
            Personas(showBackupSecurityPrompt = showPersonaPrompt)
        }

        SettingsUiState(withPersonaWarning.toPersistentList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings.toPersistentList())
    )

    fun hideImportOlympiaWalletSettingBox() {
        viewModelScope.launch {
            markImportOlympiaWalletCompleteUseCase()
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.TopLevelSettings>,
)
