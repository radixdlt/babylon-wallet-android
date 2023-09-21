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
import rdx.works.profile.data.model.apppreferences.Radix
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.gateways
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<AccountSecurityUiState>() {

    override fun initialState(): AccountSecurityUiState = AccountSecurityUiState(
        settings = persistentSetOf(
            SettingsItem.AccountSecurityAndSettingsItem.SeedPhrases,
            SettingsItem.AccountSecurityAndSettingsItem.LedgerHardwareWallets
        )
    )

    init {
        viewModelScope.launch {
            if (getProfileUseCase.gateways.first().current().network == Radix.Network.mainnet || EXPERIMENTAL_FEATURES_ENABLED) {
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
    }
}

data class AccountSecurityUiState(
    val settings: ImmutableSet<SettingsItem.AccountSecurityAndSettingsItem>
) : UiState
