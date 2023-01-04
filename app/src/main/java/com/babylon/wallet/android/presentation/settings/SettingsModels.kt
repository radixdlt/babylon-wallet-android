package com.babylon.wallet.android.presentation.settings

import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import kotlinx.collections.immutable.persistentListOf

data class SettingSection(val type: SettingSectionType, val items: List<SettingSectionItem>)

sealed class SettingSectionItem {
    object InspectProfile : SettingSectionItem()
    object AddConnection : SettingSectionItem()
    object EditGateway : SettingSectionItem()
    object DeleteAll : SettingSectionItem()

    @StringRes
    fun descriptionRes(): Int {
        return when (this) {
            AddConnection -> R.string.add_connection
            DeleteAll -> R.string.delete_all
            EditGateway -> R.string.edit_gateway
            InspectProfile -> R.string.inspect_profile
        }
    }
}

enum class SettingSectionType {
    P2P, Gateway, Account
}

val defaultAppSettings = persistentListOf(
    SettingSection(
        SettingSectionType.P2P,
        persistentListOf(SettingSectionItem.AddConnection)
    ),
    SettingSection(SettingSectionType.Gateway, persistentListOf(SettingSectionItem.EditGateway)),
    SettingSection(SettingSectionType.Account, persistentListOf(SettingSectionItem.DeleteAll))
)
