package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.data.dapp.PeerdroidClient
import com.babylon.wallet.android.domain.model.AppConstants
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.p2pLinks
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    private val peerdroidClient: PeerdroidClient
) : ViewModel(), OneOffEventHandler<SettingsEvent> by OneOffEventHandlerImpl() {

    private val defaultSettings = persistentListOf(
        SettingsItem.TopLevelSettings.LinkedConnector,
        SettingsItem.TopLevelSettings.Gateways,
        SettingsItem.TopLevelSettings.AuthorizedDapps,
        SettingsItem.TopLevelSettings.Personas,
        SettingsItem.TopLevelSettings.AppSettings,
        SettingsItem.TopLevelSettings.DeleteAll
    )

    val state = getProfileUseCase.p2pLinks()
        .map { p2pLinks ->
            val updatedSettings = if (p2pLinks.isEmpty()) {
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
            deleteProfileUseCase()
            preferencesManager.clear()
            peerdroidClient.terminate()
            sendEvent(SettingsEvent.ProfileDeleted)
        }
    }
}

internal sealed interface SettingsEvent : OneOffEvent {
    object ProfileDeleted : SettingsEvent
}

data class SettingsUiState(
    val settings: ImmutableList<SettingsItem.TopLevelSettings>
)
