package com.babylon.wallet.android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.DEBUG_MODE
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.BackupState
import rdx.works.profile.data.model.Profile
import rdx.works.profile.domain.DeleteProfileUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val peerdroidClient: PeerdroidClient,
    getProfileUseCase: GetProfileUseCase,
    getBackupStateUseCase: GetBackupStateUseCase
) : ViewModel(), OneOffEventHandler<SettingsEvent> by OneOffEventHandlerImpl() {

    private val defaultSettings = if (DEBUG_MODE) {
        persistentListOf(
            SettingsItem.TopLevelSettings.LinkedConnectors,
            SettingsItem.TopLevelSettings.Gateways,
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AppSettings,
            SettingsItem.TopLevelSettings.LedgerHardwareWallets,
            SettingsItem.TopLevelSettings.SeedPhrases,
            SettingsItem.TopLevelSettings.ImportFromLegacyWallet,
            SettingsItem.TopLevelSettings.DeleteAll
        )
    } else {
        persistentListOf(
            SettingsItem.TopLevelSettings.LinkedConnectors,
            SettingsItem.TopLevelSettings.Gateways,
            SettingsItem.TopLevelSettings.AuthorizedDapps,
            SettingsItem.TopLevelSettings.Personas,
            SettingsItem.TopLevelSettings.AppSettings,
            SettingsItem.TopLevelSettings.SeedPhrases,
            SettingsItem.TopLevelSettings.LedgerHardwareWallets,
            SettingsItem.TopLevelSettings.DeleteAll
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        getProfileUseCase(),
        getBackupStateUseCase()
    ) { profile: Profile, backupState: BackupState ->
        val showConnectionSetting = profile.appPreferences.p2pLinks.isEmpty()

        // Update connection settings based on p2p links
        val visibleSettings = if (showConnectionSetting) {
            defaultSettings.toMutableList().apply {
                if (!contains(SettingsItem.TopLevelSettings.Connection)) {
                    add(0, SettingsItem.TopLevelSettings.Connection)
                }
            }
        } else {
            defaultSettings.filter { settingSectionItem ->
                settingSectionItem != SettingsItem.TopLevelSettings.Connection
            }.toMutableList()
        }

        if (visibleSettings.any { it is SettingsItem.TopLevelSettings.Backups }) {
            visibleSettings.mapWhen(
                predicate = { it is SettingsItem.TopLevelSettings.Backups },
                mutation = {
                    SettingsItem.TopLevelSettings.Backups(backupState)
                }
            )
        } else {
            visibleSettings.add(visibleSettings.lastIndex - 2, SettingsItem.TopLevelSettings.Backups(backupState))
        }

        SettingsUiState(visibleSettings.toPersistentList())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(AppConstants.VM_STOP_TIMEOUT_MS),
        SettingsUiState(defaultSettings)
    )

    fun onDeleteWalletClick() {
        viewModelScope.launch {
            deleteProfileUseCase()
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
