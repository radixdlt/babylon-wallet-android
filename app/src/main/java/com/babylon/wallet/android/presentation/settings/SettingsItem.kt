package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.DSR
import rdx.works.profile.data.model.BackupState

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        data object LinkToConnector : TopLevelSettings
        data object ImportOlympiaWallet : TopLevelSettings
        data object AuthorizedDapps : TopLevelSettings
        data class Personas(val showBackupSecurityPrompt: Boolean = false) : TopLevelSettings
        data class AccountSecurityAndSettings(val showNotificationWarning: Boolean) : TopLevelSettings
        data object AppSettings : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                LinkToConnector -> R.string.empty
                ImportOlympiaWallet -> R.string.settings_importFromLegacyWallet
                AuthorizedDapps -> R.string.settings_authorizedDapps
                is Personas -> R.string.settings_personas
                is AccountSecurityAndSettings -> R.string.settings_accountSecurityAndSettings
                is AppSettings -> R.string.settings_appSettings
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return when (this) {
                AuthorizedDapps -> DSR.ic_authorized_dapps
                is Personas -> DSR.ic_personas
                is AccountSecurityAndSettings -> DSR.ic_security
                is AppSettings -> DSR.ic_app_settings
                else -> null
            }
        }
    }

    sealed interface AccountSecurityAndSettingsItem {
        data object SeedPhrases : AccountSecurityAndSettingsItem
        data object LedgerHardwareWallets : AccountSecurityAndSettingsItem
        data object DepositGuarantees : AccountSecurityAndSettingsItem
        data class Backups(val backupState: BackupState) : AccountSecurityAndSettingsItem
        data object ImportFromLegacyWallet : AccountSecurityAndSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                SeedPhrases -> R.string.displayMnemonics_seedPhrases
                LedgerHardwareWallets -> R.string.settings_ledgerHardwareWallets
                DepositGuarantees -> R.string.settings_depositGuarantees_title
                is Backups -> R.string.settings_backups
                ImportFromLegacyWallet -> R.string.settings_importFromLegacyWallet
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                SeedPhrases -> com.babylon.wallet.android.designsystem.R.drawable.ic_seed_phrases
                LedgerHardwareWallets -> com.babylon.wallet.android.designsystem.R.drawable.ic_ledger_hardware_wallets
                DepositGuarantees -> com.babylon.wallet.android.designsystem.R.drawable.ic_filter_list
                is Backups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
                ImportFromLegacyWallet -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
            }
        }
    }

    sealed interface AppSettingsItem {
        data object LinkedConnectors : AppSettingsItem
        data object Gateways : AppSettingsItem
        data object EntityHiding : AppSettingsItem
        data class DeveloperMode(val enabled: Boolean) : AppSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                LinkedConnectors -> R.string.settings_linkedConnectors
                Gateways -> R.string.settings_gateways
                is DeveloperMode -> R.string.appSettings_developerMode_title
                EntityHiding -> R.string.appSettings_entityHiding_title
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                LinkedConnectors -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                Gateways -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateways
                EntityHiding -> com.babylon.wallet.android.designsystem.R.drawable.ic_entity_hiding
                else -> null
            }
        }
    }
}
