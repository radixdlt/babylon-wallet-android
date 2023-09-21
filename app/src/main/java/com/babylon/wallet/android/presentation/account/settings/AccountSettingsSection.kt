package com.babylon.wallet.android.presentation.account.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R

sealed class AccountSettingsSection(val settingsItems: List<AccountSettingItem>) {

    class PersonalizeSection(settingsItems: List<AccountSettingItem>) : AccountSettingsSection(settingsItems)
    class AccountSection(settingsItems: List<AccountSettingItem>) : AccountSettingsSection(settingsItems)

    @StringRes
    fun titleRes(): Int {
        return when (this) {
            is AccountSection -> R.string.accountSettings_setBehaviorHeading
            is PersonalizeSection -> R.string.accountSettings_personalizeHeading
        }
    }
}

sealed interface AccountSettingItem {
    object AccountLabel : AccountSettingItem
    object AccountColor : AccountSettingItem
    object ShowAssetsWithTags : AccountSettingItem
    object AccountSecurity : AccountSettingItem
    object ThirdPartyDeposits : AccountSettingItem

    @StringRes
    fun titleRes(): Int {
        return when (this) {
            AccountColor -> R.string.accountSettings_accountColor
            AccountLabel -> R.string.accountSettings_accountLabel
            AccountSecurity -> R.string.accountSettings_accountSecurity
            ShowAssetsWithTags -> R.string.accountSettings_showAssets
            ThirdPartyDeposits -> R.string.accountSettings_thirdPartyDeposits
        }
    }

    @StringRes
    fun subtitleRes(): Int {
        return when (this) {
            AccountColor -> R.string.accountSettings_accountColorSubtitle
            AccountLabel -> R.string.accountSettings_accountColor_text
            AccountSecurity -> R.string.accountSettings_accountSecuritySubtitle
            ShowAssetsWithTags -> R.string.accountSettings_showAssetsSubtitle
            ThirdPartyDeposits -> R.string.accountSettings_thirdPartyDeposits
        }
    }

    @DrawableRes
    fun getIcon(): Int {
        return when (this) {
            AccountColor -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
            AccountLabel -> com.babylon.wallet.android.designsystem.R.drawable.ic_account_label
            AccountSecurity -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
            ShowAssetsWithTags -> com.babylon.wallet.android.designsystem.R.drawable.ic_tags
            ThirdPartyDeposits -> com.babylon.wallet.android.designsystem.R.drawable.ic_deposits
        }
    }
}
