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
        object ShowMnemonic : TopLevelSettings
        object ImportFromLegacyWallet : TopLevelSettings
        object DeleteAll : TopLevelSettings
        object Personas : TopLevelSettings
        data class Backups(
            val backupState: BackupState
        ) : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                Connection -> R.string.add_connection
                DeleteAll -> R.string.delete_all
                Gateways -> R.string.gateways
                InspectProfile -> R.string.inspect_profile
                LinkedConnector -> R.string.linked_connector
                Personas -> R.string.settings_personas
                AuthorizedDapps -> R.string.authorized_dapps
                AppSettings -> R.string.app_settings
                ShowMnemonic -> R.string.view_mnemonics
                ImportFromLegacyWallet -> R.string.import_from_legacy_wallet
                is Backups -> R.string.backups
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
                ImportFromLegacyWallet -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                ShowMnemonic -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
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
                is DeveloperMode -> R.string.developer_mode
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                is DeveloperMode -> R.string.warning_disables_website
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return null
        }
    }
}
