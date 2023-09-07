package com.babylon.wallet.android.presentation.settings.accountsecurity

import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor() : StateViewModel<AccountSecurityUiState>() {

    override fun initialState(): AccountSecurityUiState = AccountSecurityUiState.default
}

data class AccountSecurityUiState(
    val settings: ImmutableSet<SettingsItem.AccountSecurityAndSettingsItem>
) : UiState {

    companion object {
        val default = AccountSecurityUiState(
            settings = persistentSetOf(
                SettingsItem.AccountSecurityAndSettingsItem.SeedPhrases,
                SettingsItem.AccountSecurityAndSettingsItem.LedgerHardwareWallets,
                SettingsItem.AccountSecurityAndSettingsItem.ImportFromLegacyWallet
            )
        )
    }
}
