package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.BuildConfig.EXPERIMENTAL_FEATURES_ENABLED
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.backup.GetBackupStateUseCase
import rdx.works.profile.domain.gateways
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getBackupStateUseCase: GetBackupStateUseCase
) : StateViewModel<AccountSecurityUiState>() {

    override fun initialState(): AccountSecurityUiState = AccountSecurityUiState(
        settings = persistentSetOf(
            SettingsItem.AccountSecurityAndSettingsItem.SeedPhrases,
            SettingsItem.AccountSecurityAndSettingsItem.LedgerHardwareWallets,
            SettingsItem.AccountSecurityAndSettingsItem.DepositGuarantees,
        )
    )

    init {
        viewModelScope.launch {
            if (getProfileUseCase.gateways.first().current().network.id == Radix.Network.mainnet.id || EXPERIMENTAL_FEATURES_ENABLED) {
                _state.update { state ->
                    state.copy(
                        settings = state.settings
                            .toMutableList()
                            .apply { add(SettingsItem.AccountSecurityAndSettingsItem.ImportFromLegacyWallet) }
                            .toPersistentSet()
                    )
                }
            }
        }
        viewModelScope.launch {
            getBackupStateUseCase().collect { backupState ->
                _state.update { settingsUiState ->
                    val settingsList = if (settingsUiState.settings.any { it is SettingsItem.AccountSecurityAndSettingsItem.Backups }) {
                        settingsUiState.settings.mapWhen(
                            predicate = { it is SettingsItem.AccountSecurityAndSettingsItem.Backups },
                            mutation = { SettingsItem.AccountSecurityAndSettingsItem.Backups(backupState = backupState) }
                        )
                    } else {
                        settingsUiState.settings.toMutableList().apply {
                            add(3, SettingsItem.AccountSecurityAndSettingsItem.Backups(backupState))
                        }
                    }
                    settingsUiState.copy(settings = settingsList.toPersistentSet())
                }
            }
        }
    }
}

data class AccountSecurityUiState(
    val settings: ImmutableSet<SettingsItem.AccountSecurityAndSettingsItem>
) : UiState
