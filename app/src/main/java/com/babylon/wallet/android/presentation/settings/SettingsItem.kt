package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.presentation.ui.composables.DSR
import rdx.works.profile.data.model.BackupState

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        data object LinkToConnector : TopLevelSettings

        data object SecurityCenter : TopLevelSettings
        data object Personas : TopLevelSettings
        data object ApprovedDapps : TopLevelSettings

        data object LinkedConnectors : TopLevelSettings
        data object Preferences : TopLevelSettings

        data object Troubleshooting : TopLevelSettings
        data object DebugSettings : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                LinkToConnector -> R.string.empty
                ApprovedDapps -> R.string.settings_authorizedDapps
                is Personas -> R.string.settings_personas
                is Preferences -> R.string.appSettings_preferences
                is DebugSettings -> R.string.settings_debugSettings
                LinkedConnectors -> R.string.appSettings_linkedConnectors
                SecurityCenter -> R.string.appSettings_securityCenter
                Troubleshooting -> R.string.appSettings_troubleshooting
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                LinkToConnector -> R.string.empty
                ApprovedDapps -> R.string.appSettings_approvedDapps_subtitle
                is Personas -> R.string.appSettings_personas_subtitle
                is Preferences -> R.string.appSettings_preferences_subtitle
                is DebugSettings -> R.string.settings_debugSettings
                LinkedConnectors -> R.string.appSettings_linkedConnectors_subtitle
                SecurityCenter -> R.string.appSettings_securityCenter_subtitle
                Troubleshooting -> R.string.appSettings_troubleshooting_subtitle
            }
        }

        @DrawableRes
        fun getIcon(): Int? {
            return when (this) {
                ApprovedDapps -> DSR.ic_authorized_dapps
                is Personas -> DSR.ic_personas
                is Preferences -> DSR.ic_filter_list
                is DebugSettings -> DSR.ic_app_settings
                LinkedConnectors -> DSR.ic_desktop_connection
                SecurityCenter -> DSR.ic_security_center
                Troubleshooting -> DSR.ic_troubleshooting
                else -> null
            }
        }
    }

    sealed interface SecurityFactorsSettingsItem {

        val count: Int

        data class SeedPhrases(override val count: Int, val needsRecovery: Boolean) : SecurityFactorsSettingsItem
        data class LedgerHardwareWallets(override val count: Int) : SecurityFactorsSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                is SeedPhrases -> R.string.displayMnemonics_seedPhrases
                is LedgerHardwareWallets -> R.string.accountSecuritySettings_ledgerHardwareWallets_title
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                is SeedPhrases -> R.string.securitySettings_seedPhrasesSubtitle
                is LedgerHardwareWallets -> R.string.securitySettings_ledgerHardwareWalletsSubtitle
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                is SeedPhrases -> DSR.ic_seed_phrases
                is LedgerHardwareWallets -> DSR.ic_ledger_hardware_wallets
            }
        }
    }

    sealed interface AccountSecurityAndSettingsItem {
        data class Backups(val backupState: BackupState) : AccountSecurityAndSettingsItem
        data object ImportFromLegacyWallet : AccountSecurityAndSettingsItem
        data object AccountRecovery : AccountSecurityAndSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                is Backups -> R.string.accountSecuritySettings_backups_title
                ImportFromLegacyWallet -> R.string.accountSecuritySettings_importFromLegacyWallet_title
                AccountRecovery -> R.string.accountSecuritySettings_accountRecoveryScan_title
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                is Backups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
                ImportFromLegacyWallet -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
                AccountRecovery -> com.babylon.wallet.android.designsystem.R.drawable.ic_app_settings
            }
        }
    }

    sealed interface WalletPreferencesSettingsItem {
        data object DepositGuarantees : WalletPreferencesSettingsItem
        data object EntityHiding : WalletPreferencesSettingsItem
        data object Gateways : WalletPreferencesSettingsItem
        data class DeveloperMode(val enabled: Boolean) : WalletPreferencesSettingsItem
        data class CrashReporting(val enabled: Boolean) : WalletPreferencesSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                DepositGuarantees -> R.string.accountSecuritySettings_depositGuarantees_title
                Gateways -> R.string.appSettings_gateways_title
                is DeveloperMode -> R.string.appSettings_developerMode_title
                EntityHiding -> R.string.appSettings_entityHiding_title
                is CrashReporting -> R.string.appSettings_crashReporting_title
            }
        }

        @StringRes
        fun subtitleRes(): Int? {
            return when (this) {
                DepositGuarantees -> R.string.walletPreferencesSettings_defaultDeposit_subtitle
                Gateways -> null
                is DeveloperMode -> R.string.appSettings_developerMode_subtitle
                EntityHiding -> R.string.walletPreferencesSettings_entityHiding_subtitle
                is CrashReporting -> null
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                Gateways -> com.babylon.wallet.android.designsystem.R.drawable.ic_gateways
                EntityHiding -> com.babylon.wallet.android.designsystem.R.drawable.ic_entity
                DepositGuarantees -> DSR.ic_filter_list
                else -> null
            }
        }
    }

    sealed interface DebugSettingsItem {
        data object InspectProfile : DebugSettingsItem

        data object LinkConnectionStatusIndicator : DebugSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                InspectProfile -> R.string.settings_debugSettings_inspectProfile
                LinkConnectionStatusIndicator -> R.string.linkedConnectors_title
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                InspectProfile -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                LinkConnectionStatusIndicator -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
            }
        }

        companion object {
            fun values() = setOf(
                InspectProfile,
                LinkConnectionStatusIndicator
            )
        }
    }
}
