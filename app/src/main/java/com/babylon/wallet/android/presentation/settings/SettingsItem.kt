package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import rdx.works.profile.data.model.BackupState

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        object LinkToConnector : TopLevelSettings
        object ImportOlympiaWallet : TopLevelSettings
        object AuthorizedDapps : TopLevelSettings
        object Personas : TopLevelSettings
        object AccountSecurityAndSettings : TopLevelSettings
        object AppSettings : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                LinkToConnector -> R.string.empty
                ImportOlympiaWallet -> R.string.settings_importFromLegacyWallet
                AuthorizedDapps -> R.string.settings_authorizedDapps
                Personas -> R.string.settings_personas
                AccountSecurityAndSettings -> R.string.accountSettings_accountSecurity
                is AppSettings -> R.string.settings_appSettings
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                AuthorizedDapps -> com.babylon.wallet.android.designsystem.R.drawable.ic_authorized_dapps
                Personas -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                is AppSettings -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                else -> null
            }
        }
    }

    sealed interface AccountSecurityAndSettingsItem {
        object SeedPhrases : AccountSecurityAndSettingsItem
        object LedgerHardwareWallets : AccountSecurityAndSettingsItem
        object DepositGuarantees : AccountSecurityAndSettingsItem
        object ImportFromLegacyWallet : AccountSecurityAndSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                SeedPhrases -> R.string.displayMnemonics_seedPhrases
                LedgerHardwareWallets -> R.string.settings_ledgerHardwareWallets
                DepositGuarantees -> R.string.accountSettings_thirdPartyDeposits
                ImportFromLegacyWallet -> R.string.settings_importFromLegacyWallet
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                SeedPhrases -> com.babylon.wallet.android.designsystem.R.drawable.ic_seed_phrases
                LedgerHardwareWallets -> com.babylon.wallet.android.designsystem.R.drawable.ic_ledger_hardware_wallets
                ImportFromLegacyWallet -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                else -> null
            }
        }
    }

    sealed interface AppSettingsItem {
        object LinkedConnectors : AppSettingsItem
        object Gateways : AppSettingsItem
        data class Backups(val backupState: BackupState) : AppSettingsItem
        data class DeveloperMode(val enabled: Boolean) : AppSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                LinkedConnectors -> R.string.settings_linkedConnectors
                Gateways -> R.string.settings_gateways
                is Backups -> R.string.settings_backups
                is DeveloperMode -> R.string.appSettings_developerMode_title
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                LinkedConnectors -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                Gateways -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateways
                is Backups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
                else -> null
            }
        }
    }

//    sealed interface AppSettings {
//        data class DeveloperMode(val enabled: Boolean) : AppSettings
//
//        @StringRes
//        fun descriptionRes(): Int {
//            return when (this) {
//                is DeveloperMode -> R.string.appSettings_developerMode_title
//            }
//        }
//
//        @StringRes
//        fun subtitleRes(): Int {
//            return when (this) {
//                is DeveloperMode -> R.string.appSettings_developerMode_subtitle
//            }
//        }
//
//        @DrawableRes
//        fun getIcon(): Int? {
//            return null
//        }
//    }
}
