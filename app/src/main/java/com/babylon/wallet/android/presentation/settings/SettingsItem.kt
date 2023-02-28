package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        object InspectProfile : TopLevelSettings
        object Connection : TopLevelSettings
        object LinkedConnector : TopLevelSettings
        object Gateway : TopLevelSettings
        object ConnectedDapps : TopLevelSettings
        object AppSettings : TopLevelSettings
        object DeleteAll : TopLevelSettings
        object Personas : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                Connection -> R.string.add_connection
                DeleteAll -> R.string.delete_all
                Gateway -> R.string.network_gateway
                InspectProfile -> R.string.inspect_profile
                LinkedConnector -> R.string.linked_connector
                Personas -> R.string.settings_personas
                ConnectedDapps -> R.string.connected_dapps
                AppSettings -> R.string.app_settings
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return when (this) {
                Gateway -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateway
                LinkedConnector -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                Personas -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                ConnectedDapps -> com.babylon.wallet.android.designsystem.R.drawable.ic_connected_dapps
                AppSettings -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
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
