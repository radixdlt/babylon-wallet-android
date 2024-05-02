package com.babylon.wallet.android.presentation.account.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.radixdlt.sargon.DepositRule

sealed class AccountSettingsSection(open val settingsItems: List<AccountSettingItem>) {

    data class PersonalizeSection(override val settingsItems: List<AccountSettingItem>) : AccountSettingsSection(settingsItems)
    data class AccountSection(override val settingsItems: List<AccountSettingItem>) : AccountSettingsSection(settingsItems)
    data class DevelopmentSection(override val settingsItems: List<AccountSettingItem>) : AccountSettingsSection(settingsItems)

    @StringRes
    fun titleRes(): Int {
        return when (this) {
            is AccountSection -> R.string.accountSettings_setBehaviorHeading
            is PersonalizeSection -> R.string.accountSettings_personalizeHeading
            is DevelopmentSection -> R.string.accountSettings_developmentHeading
        }
    }
}

sealed interface AccountSettingItem {
    data object AccountLabel : AccountSettingItem
    data object AccountColor : AccountSettingItem
    data object ShowAssetsWithTags : AccountSettingItem
    data object AccountSecurity : AccountSettingItem
    data class ThirdPartyDeposits(val defaultDepositRule: DepositRule) : AccountSettingItem
    data object DevSettings : AccountSettingItem

    @StringRes
    fun titleRes(): Int {
        return when (this) {
            AccountColor -> R.string.accountSettings_accountColor
            AccountLabel -> R.string.accountSettings_accountLabel
            AccountSecurity -> R.string.settings_accountSecurityAndSettings
            ShowAssetsWithTags -> R.string.accountSettings_showAssets
            is ThirdPartyDeposits -> R.string.accountSettings_thirdPartyDeposits
            DevSettings -> R.string.accountSettings_devPreferences
        }
    }

    @StringRes
    fun subtitleRes(): Int {
        return when (this) {
            AccountColor -> R.string.accountSettings_accountColorSubtitle
            AccountLabel -> R.string.accountSettings_accountColor_text
            AccountSecurity -> R.string.accountSettings_setBehaviorHeading
            ShowAssetsWithTags -> R.string.accountSettings_showAssetsSubtitle
            is ThirdPartyDeposits -> R.string.accountSettings_thirdPartyDeposits
            DevSettings -> R.string.accountSettings_devPreferences
        }
    }

    @DrawableRes
    fun getIcon(): Int {
        return when (this) {
            AccountColor -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
            AccountLabel -> com.babylon.wallet.android.designsystem.R.drawable.ic_account_label
            AccountSecurity -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
            ShowAssetsWithTags -> com.babylon.wallet.android.designsystem.R.drawable.ic_tags
            is ThirdPartyDeposits -> com.babylon.wallet.android.designsystem.R.drawable.ic_deposits
            DevSettings -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
        }
    }
}
