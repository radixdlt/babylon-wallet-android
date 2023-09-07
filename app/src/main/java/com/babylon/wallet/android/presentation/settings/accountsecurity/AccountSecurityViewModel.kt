package com.babylon.wallet.android.presentation.settings.accountsecurity

import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.settings.SettingsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import javax.inject.Inject

@HiltViewModel
class AccountSecurityViewModel @Inject constructor() : StateViewModel<AccountSecurityUiState>() {

    override fun initialState(): AccountSecurityUiState = AccountSecurityUiState(
        settings = mutableListOf(
            SettingsItem.AccountSecurityAndSettingsItem.SeedPhrases,
            SettingsItem.AccountSecurityAndSettingsItem.LedgerHardwareWallets,
        ).apply {
            if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
                add(SettingsItem.AccountSecurityAndSettingsItem.ImportFromLegacyWallet)
            }
        }.toPersistentSet()
    )
}

data class AccountSecurityUiState(
    val settings: ImmutableSet<SettingsItem.AccountSecurityAndSettingsItem>
) : UiState
