package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val preferencesManager: PreferencesManager,
    private val peerdroidClient: PeerdroidClient
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileDataSource.p2pClient.collect { p2pClient ->
                val updatedSettings = if (p2pClient == null) {
                    state.value.settings.toMutableList().apply {
                        if (!contains(SettingSectionItem.Connection)) {
                            add(0, SettingSectionItem.Connection)
                        }
                    }
                } else {
                    state.value.settings.filter { settingSectionItem ->
                        settingSectionItem != SettingSectionItem.Connection
                    }
                }
                _state.update {
                    it.copy(settings = updatedSettings.toPersistentList())
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.developerMode.collect { skip ->
                val index = state.value.settings.indexOfFirst { it is SettingSectionItem.DeveloperMode }

                _state.update { state ->
                    state.copy(
                        settings = state.settings.toMutableList().apply {
                            if (index != -1) {
                                removeAt(index)
                                add(index, SettingSectionItem.DeveloperMode(skip))
                            }
                        }.toPersistentList()
                    )
                }
            }
        }
    }

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
    val settings: ImmutableList<SettingSectionItem> = persistentListOf(
        SettingSectionItem.LinkedConnector,
        SettingSectionItem.Gateway,
        SettingSectionItem.Personas,
        SettingSectionItem.DeveloperMode(false),
        SettingSectionItem.DeleteAll
    )
)
