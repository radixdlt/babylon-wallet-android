package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.utils.Constants.RADIX_SUPPORT_EMAIL_ADDRESS
import com.babylon.wallet.android.utils.Constants.RADIX_SUPPORT_EMAIL_SUBJECT
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        data class SecurityCenter(val securityProblems: Set<SecurityProblem> = emptySet()) : TopLevelSettings
        data class Personas(
            val isCloudBackupNotWorking: SecurityProblem.CloudBackupNotWorking? = null, // security problem: 5,6,7
            val isBackupNeeded: Boolean = false, // security problem 3
            val isRecoveryNeeded: Boolean = false // security problem 9
        ) : TopLevelSettings

        data object ApprovedDapps : TopLevelSettings

        data object LinkedConnectors : TopLevelSettings
        data object Preferences : TopLevelSettings

        data object Troubleshooting : TopLevelSettings
        data object DebugSettings : TopLevelSettings

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                ApprovedDapps -> R.string.walletSettings_dapps_title
                is Personas -> R.string.walletSettings_personas_title
                is Preferences -> R.string.walletSettings_preferences_title
                is DebugSettings -> R.string.settings_debugSettings
                LinkedConnectors -> R.string.walletSettings_connectors_title
                is SecurityCenter -> R.string.walletSettings_securityCenter_title
                Troubleshooting -> R.string.walletSettings_troubleshooting_title
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                ApprovedDapps -> R.string.walletSettings_dapps_subtitle
                is Personas -> R.string.walletSettings_personas_subtitle
                is Preferences -> R.string.walletSettings_preferences_subtitle
                is DebugSettings -> R.string.settings_debugSettings
                LinkedConnectors -> R.string.walletSettings_connectors_subtitle
                is SecurityCenter -> R.string.walletSettings_securityCenter_subtitle
                Troubleshooting -> R.string.walletSettings_troubleshooting_subtitle
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
                is SecurityCenter -> DSR.ic_security_center
                Troubleshooting -> DSR.ic_troubleshooting
                else -> null
            }
        }
    }

    sealed interface SecurityFactorsSettingsItem {

        val count: Int

        data class SeedPhrases(
            override val count: Int,
            val securityProblems: ImmutableSet<SecurityProblem> = persistentSetOf()
        ) : SecurityFactorsSettingsItem

        data class LedgerHardwareWallets(override val count: Int) : SecurityFactorsSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                is SeedPhrases -> R.string.securityFactors_seedPhrases_title
                is LedgerHardwareWallets -> R.string.securityFactors_ledgerWallet_title
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                is SeedPhrases -> R.string.securityFactors_seedPhrases_subtitle
                is LedgerHardwareWallets -> R.string.securityFactors_ledgerWallet_subtitle
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

    sealed interface Troubleshooting {
        data object AccountRecovery : Troubleshooting
        data object ImportFromLegacyWallet : Troubleshooting
        data class ContactSupport(
            val body: String,
            val supportAddress: String = RADIX_SUPPORT_EMAIL_ADDRESS,
            val subject: String = RADIX_SUPPORT_EMAIL_SUBJECT
        ) : Troubleshooting

        data object Discord : Troubleshooting
        data object FactoryReset : Troubleshooting

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                ImportFromLegacyWallet -> R.string.troubleshooting_legacyImport_title
                AccountRecovery -> R.string.troubleshooting_accountScan_title
                is ContactSupport -> R.string.troubleshooting_contactSupport_title
                Discord -> R.string.troubleshooting_discord_title
                FactoryReset -> R.string.troubleshooting_factoryReset_title
            }
        }

        @StringRes
        fun subtitleRes(): Int {
            return when (this) {
                ImportFromLegacyWallet -> R.string.troubleshooting_legacyImport_subtitle
                AccountRecovery -> R.string.troubleshooting_accountScan_subtitle
                is ContactSupport -> R.string.troubleshooting_contactSupport_subtitle
                Discord -> R.string.troubleshooting_discord_subtitle
                FactoryReset -> R.string.troubleshooting_factoryReset_subtitle
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                ImportFromLegacyWallet -> DSR.ic_recovery
                AccountRecovery -> DSR.ic_recovery
                is ContactSupport -> DSR.ic_email
                Discord -> DSR.ic_discord
                FactoryReset -> DSR.ic_factory_reset
            }
        }
    }

    sealed interface WalletPreferences {
        data object DepositGuarantees : WalletPreferences
        data object EntityHiding : WalletPreferences
        data object Gateways : WalletPreferences
        data class DeveloperMode(val enabled: Boolean) : WalletPreferences
        data class CrashReporting(val enabled: Boolean) : WalletPreferences
        data class EnableAppLockInBackground(val enabled: Boolean) : WalletPreferences

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                DepositGuarantees -> R.string.preferences_depositGuarantees_title
                Gateways -> R.string.preferences_gateways
                is DeveloperMode -> R.string.appSettings_developerMode_title
                EntityHiding -> R.string.preferences_hiddenEntities_title
                is CrashReporting -> R.string.appSettings_crashReporting_title
                is EnableAppLockInBackground -> R.string.settings_debugSettings_appLock
            }
        }

        @StringRes
        fun subtitleRes(): Int? {
            return when (this) {
                DepositGuarantees -> R.string.preferences_depositGuarantees_subtitle
                Gateways -> null
                is DeveloperMode -> R.string.appSettings_developerMode_subtitle
                EntityHiding -> R.string.preferences_hiddenEntities_subtitle
                is CrashReporting -> null
                is EnableAppLockInBackground -> DSR.ic_backup
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                Gateways -> DSR.ic_gateways
                EntityHiding -> DSR.ic_entity_hiding
                DepositGuarantees -> DSR.ic_filter_list
                is DeveloperMode -> DSR.ic_developer_mode
                is EnableAppLockInBackground -> DSR.ic_app_settings
                else -> null
            }
        }
    }

    sealed interface DebugSettingsItem {
        data object InspectProfile : DebugSettingsItem

        data object LinkConnectionStatusIndicator : DebugSettingsItem

        data object InspectCloudBackups : DebugSettingsItem

        @StringRes
        fun descriptionRes(): Int {
            return when (this) {
                InspectProfile -> R.string.settings_debugSettings_inspectProfile
                LinkConnectionStatusIndicator -> R.string.linkedConnectors_title
                InspectCloudBackups -> R.string.settings_debugSettings_inspectCloudBackups
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                InspectProfile -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                LinkConnectionStatusIndicator -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                InspectCloudBackups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
            }
        }

        companion object {
            fun values() = setOf(
                InspectProfile,
                LinkConnectionStatusIndicator,
                InspectCloudBackups
            )
        }
    }
}
