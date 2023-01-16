package com.babylon.wallet.android.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileDataSource: ProfileDataSource,
    private val preferencesManager: PreferencesManager,
    private val peerdroidClient: PeerdroidClient
) : ViewModel() {

    var state by mutableStateOf(SettingsUiState())
        private set

    init {
        viewModelScope.launch {
            profileDataSource.p2pClient.collect { p2pClient ->
                val updatedSettings = if (p2pClient == null) {
                    state.settings.toMutableList().apply {
                        if (!contains(SettingSectionItem.Connection)) {
                            add(0, SettingSectionItem.Connection)
                        }
                    }
                } else {
                    state.settings.filter { it != SettingSectionItem.Connection }
                }
                state = state.copy(settings = updatedSettings.toPersistentList())
            }
        }
    }

    fun onDeleteWalletClick() {
        viewModelScope.launch {
            profileDataSource.clear()
            preferencesManager.clear()
            peerdroidClient.close()
        }
    }
}

data class SettingsUiState(
    val settings: ImmutableList<SettingSectionItem> = persistentListOf(
        SettingSectionItem.LinkedConnector,
        SettingSectionItem.Gateway,
        SettingSectionItem.DeleteAll
    )
)
