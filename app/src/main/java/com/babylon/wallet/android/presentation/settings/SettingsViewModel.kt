package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.AppConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val preferencesManager: PreferencesManager,
    private val peerdroidClient: PeerdroidClient
) : ViewModel() {

    private val defaultSettings = persistentListOf(
        SettingsItem.TopLevelSettings.LinkedConnector,
        SettingsItem.TopLevelSettings.Gateway,
        SettingsItem.TopLevelSettings.ConnectedDapps,
        SettingsItem.TopLevelSettings.Personas,
        SettingsItem.TopLevelSettings.AppSettings,
        SettingsItem.TopLevelSettings.DeleteAll
    )

    val state = profileDataSource.p2pClient.map { p2pClient ->
        val updatedSettings = if (p2pClient == null) {
            defaultSettings.toMutableList().apply {
                if (!contains(SettingsItem.TopLevelSettings.Connection)) {
                    add(0, SettingsItem.TopLevelSettings.Connection)
                }
            }
        } else {
            defaultSettings.filter { settingSectionItem ->
                settingSectionItem != SettingsItem.TopLevelSettings.Connection
            }
        }.toPersistentList()
        SettingsUiState(updatedSettings)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings)
    )

    fun onDeleteWalletClick() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.TopLevelSettings>
)
