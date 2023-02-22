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
import kotlinx.coroutines.flow.combine
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
        SettingSectionItem.LinkedConnector,
        SettingSectionItem.Gateway,
        SettingSectionItem.Personas,
        SettingSectionItem.DeveloperMode(false),
        SettingSectionItem.DeleteAll
    )

    val state = combine(profileDataSource.p2pClient, preferencesManager.developerMode) { p2pClient, developerMode ->
        val updatedSettings = if (p2pClient == null) {
            defaultSettings.toMutableList().apply {
                if (!contains(SettingSectionItem.Connection)) {
                    add(0, SettingSectionItem.Connection)
                }
            }
        } else {
            defaultSettings.filter { settingSectionItem ->
                settingSectionItem != SettingSectionItem.Connection
            }
        }.toMutableList().apply {
            val index = indexOfFirst { it is SettingSectionItem.DeveloperMode }
            if (index != -1) {
                removeAt(index)
                add(index, SettingSectionItem.DeveloperMode(developerMode))
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

    fun onDeleteWalletClick() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.close(shouldCloseConnectionToSignalingServer = true)
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingSectionItem>
)
