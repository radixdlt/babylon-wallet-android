package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.utils.AppConstants
import com.babylon.wallet.android.domain.usecases.settings.GetImportOlympiaSettingVisibilityUseCase
import com.babylon.wallet.android.domain.usecases.settings.MarkImportOlympiaWalletCompleteUseCase
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AccountSecurityAndSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AppSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.AuthorizedDapps
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.ImportOlympiaWallet
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.LinkToConnector
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Personas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.Profile
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.data.model.currentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase,
    getImportOlympiaSettingVisibilityUseCase: GetImportOlympiaSettingVisibilityUseCase,
    private val markImportOlympiaWalletCompleteUseCase: MarkImportOlympiaWalletCompleteUseCase
) : ViewModel() {

    private val defaultSettings = listOf(
        AuthorizedDapps,
        Personas,
        AccountSecurityAndSettings,
        AppSettings
    )

    val state: StateFlow<SettingsUiState> = combine(
        getProfileUseCase(),
        getImportOlympiaSettingVisibilityUseCase()
    ) { profile: Profile, isImportFromOlympiaSettingDismissed ->
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
        SettingsUiState(mutated.toPersistentList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings.toPersistentList())
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
