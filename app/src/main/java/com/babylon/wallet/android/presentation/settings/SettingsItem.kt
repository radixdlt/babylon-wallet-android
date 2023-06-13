package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import rdx.works.profile.data.model.BackupState

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        object InspectProfile : TopLevelSettings
        object Connection : TopLevelSettings
        object LinkedConnector : TopLevelSettings
        object Gateways : TopLevelSettings
        object AuthorizedDapps : TopLevelSettings
        object AppSettings : TopLevelSettings
        object SeedPhrases : TopLevelSettings
        object LedgerHardwareWallets : TopLevelSettings
        object ImportFromLegacyWallet : TopLevelSettings
        object DeleteAll : TopLevelSettings
        object Personas : TopLevelSettings
        data class Backups(
            val backupState: BackupState
        ) : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                Connection -> R.string.empty
                DeleteAll -> R.string.settings_deleteWalletData
                Gateways -> R.string.settings_gateways
                InspectProfile -> R.string.settings_inspectProfile
                LinkedConnector -> R.string.settings_linkedConnectors
                Personas -> R.string.settings_personas
                AuthorizedDapps -> R.string.settings_authorizedDapps
                AppSettings -> R.string.settings_appSettings
                SeedPhrases -> R.string.displayMnemonics_seedPhrases
                ImportFromLegacyWallet -> R.string.settings_importFromLegacyWallet
                LedgerHardwareWallets -> R.string.settings_ledgerHardwareWallets
                is Backups -> R.string.settings_backups
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return when (this) {
                Gateways -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateways
                LinkedConnector -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                Personas -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                AuthorizedDapps -> com.babylon.wallet.android.designsystem.R.drawable.ic_authorized_dapps
                AppSettings -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                SeedPhrases -> com.babylon.wallet.android.designsystem.R.drawable.ic_seed_phrases
                LedgerHardwareWallets -> com.babylon.wallet.android.designsystem.R.drawable.ic_hardware_ledger
                ImportFromLegacyWallet -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                is Backups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
                else -> null
            }
        }
    }

    sealed interface AppSettings {
        data class DeveloperMode(val enabled: Boolean) : AppSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                is DeveloperMode -> R.string.generalSettings_developerMode_title
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                is DeveloperMode -> R.string.generalSettings_developerMode_subtitle
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return null
        }
    }
}
