package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.DebugSettings
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.LinkToConnector
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Personas
import com.babylon.wallet.android.presentation.settings.SettingsItem.TopLevelSettings.Preferences
import com.babylon.wallet.android.utils.Constants
import com.radixdlt.sargon.SargonBuildInformation
import com.radixdlt.sargon.extensions.Sargon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import rdx.works.profile.data.model.Profile
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val defaultSettings = listOf(
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.SecurityCenter),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(Personas),
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.ApprovedDapps),
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.LinkedConnectors),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(Preferences),
        SettingsUiItem.Spacer,
        SettingsUiItem.Settings(SettingsItem.TopLevelSettings.Troubleshooting),
        if (EXPERIMENTAL_FEATURES_ENABLED) SettingsUiItem.Settings(DebugSettings) else null
    ).mapNotNull { it }

    val state: StateFlow<SettingsUiState> = getProfileUseCase().map { profile: Profile ->
        val mutated = defaultSettings.toMutableList()
        var topIndex = 0
        if (profile.appPreferences.p2pLinks.isEmpty() && !defaultSettings.filterIsInstance<SettingsUiItem.Settings>().map {
                it.item
            }.contains(LinkToConnector)
        ) {
            mutated.add(topIndex, SettingsUiItem.Settings(LinkToConnector))
            topIndex += 1
        }
        SettingsUiState(mutated.toPersistentList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(Constants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings.toPersistentList())
    )
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.TopLevelSettings>
) {

    val debugBuildInformation: DebugBuildInformation?
        get() = if (EXPERIMENTAL_FEATURES_ENABLED) DebugBuildInformation else null
}

data object DebugBuildInformation {

    const val SIGNALING_SERVER: String = rdx.works.peerdroid.BuildConfig.SIGNALING_SERVER_URL
    val sargonInfo: SargonBuildInformation = Sargon.buildInformation.apply {
        Timber.d(this.dependencies.toString())
    }
}

sealed interface SettingsUiItem {
    data object Spacer : SettingsUiItem
    data class Settings(val item: SettingsItem.TopLevelSettings) : SettingsUiItem
}
