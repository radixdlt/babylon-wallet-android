package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R

data class SettingSection(val type: SettingSectionType, val items: List<SettingSectionItem>)

sealed class SettingSectionItem {
    object InspectProfile : SettingSectionItem()
    object Connection : SettingSectionItem()
    object LinkedConnector : SettingSectionItem()
    object Gateway : SettingSectionItem()
    object DeleteAll : SettingSectionItem()

    object Personas : SettingSectionItem()

    @StringRes
    fun descriptionRes(): Int {
        return when (this) {
            Connection -> R.string.add_connection
            DeleteAll -> R.string.delete_all
            Gateway -> R.string.network_gateway
            InspectProfile -> R.string.inspect_profile
            LinkedConnector -> R.string.linked_connector
            Personas -> R.string.settings_personas
        }
    }

    @DrawableRes
    fun getIcon(): Int? {
        return when (this) {
            Gateway -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateway
            LinkedConnector -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
            else -> null
        }
    }
}

enum class SettingSectionType {
    Gateway, Account
}
