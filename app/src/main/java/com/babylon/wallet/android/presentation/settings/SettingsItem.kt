package com.babylon.wallet.android.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.babylon.wallet.android.R
import com.babylon.wallet.android.domain.model.SecurityProblem
import com.babylon.wallet.android.presentation.ui.composables.DSR
import com.babylon.wallet.android.utils.Constants.RADIX_SUPPORT_EMAIL_ADDRESS
import com.babylon.wallet.android.utils.Constants.RADIX_SUPPORT_EMAIL_SUBJECT
import com.babylon.wallet.android.utils.Flavors
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import rdx.works.core.domain.ThemeSelection

sealed interface SettingsItem {

    sealed interface TopLevelSettings {
        data class SecurityCenter(val securityProblems: Set<SecurityProblem> = emptySet()) : TopLevelSettings
        data class Personas(
            val isCloudBackupNotWorking: SecurityProblem.CloudBackupNotWorking? = null, // security problem: 5,6,7
            val isBackupNeeded: Boolean = false, // security problem 3
            val isRecoveryNeeded: Boolean = false // security problem 9
        ) : TopLevelSettings

        data object LinkedConnectors : TopLevelSettings
        data object Preferences : TopLevelSettings

        data object Troubleshooting : TopLevelSettings
        data object DebugSettings : TopLevelSettings

        @StringRes
        fun titleRes(): Int {
            return when (this) {
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
                is Personas -> DSR.ic_personas
                is Preferences -> DSR.ic_filter_list
                is DebugSettings -> DSR.ic_app_settings
                LinkedConnectors -> DSR.ic_desktop_connection
                is SecurityCenter -> DSR.ic_security_center
                Troubleshooting -> DSR.ic_troubleshooting
            }
        }
    }

    sealed interface SecurityFactorsSettingsItem {

        data class BiometricsPin(
            val securityProblems: ImmutableSet<SecurityProblem> = persistentSetOf()
        ) : SecurityFactorsSettingsItem

        data object LedgerNano : SecurityFactorsSettingsItem
        data object ArculusCard : SecurityFactorsSettingsItem
        data object Password : SecurityFactorsSettingsItem
        data object OffDeviceMnemonic : SecurityFactorsSettingsItem
    }

    sealed interface Troubleshooting {
        data object AccountRecovery : Troubleshooting
        data object ImportFromLegacyWallet : Troubleshooting
        data class ContactSupport(
            val body: String,
            val supportAddress: String = RADIX_SUPPORT_EMAIL_ADDRESS,
            val subject: String = RADIX_SUPPORT_EMAIL_SUBJECT
        ) : Troubleshooting

        data object ExportLogs : Troubleshooting
        data object FactoryReset : Troubleshooting
        data object SendTransactionManifest : Troubleshooting

        @Composable
        fun title(): String = when (this) {
            ImportFromLegacyWallet -> stringResource(id = R.string.troubleshooting_legacyImport_title)
            AccountRecovery -> stringResource(id = R.string.troubleshooting_accountScan_title)
            is ContactSupport -> stringResource(id = R.string.troubleshooting_contactSupport_title)
            ExportLogs -> stringResource(id = R.string.export_logs_title)
            FactoryReset -> stringResource(id = R.string.troubleshooting_factoryReset_title)
            SendTransactionManifest -> "Submit Transaction Manifest"
        }

        @Composable
        fun subtitle(): String = when (this) {
            ImportFromLegacyWallet -> stringResource(id = R.string.troubleshooting_legacyImport_subtitle)
            AccountRecovery -> stringResource(id = R.string.troubleshooting_accountScan_subtitle)
            is ContactSupport -> stringResource(id = R.string.troubleshooting_contactSupport_subtitle)
            ExportLogs -> stringResource(id = R.string.export_logs_message)
            FactoryReset -> stringResource(id = R.string.troubleshooting_factoryReset_subtitle)
            SendTransactionManifest -> "Enter a raw transaction manifest to preview and submit to the network."
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                ImportFromLegacyWallet -> DSR.ic_recovery
                AccountRecovery -> DSR.ic_recovery
                is ContactSupport -> DSR.ic_email
                ExportLogs -> DSR.ic_app_settings
                FactoryReset -> DSR.ic_factory_reset
                SendTransactionManifest -> DSR.ic_manifest_expand
            }
        }
    }

    sealed interface WalletPreferences {
        data object DepositGuarantees : WalletPreferences
        data object EntityHiding : WalletPreferences
        data object AssetsHiding : WalletPreferences
        data object Gateways : WalletPreferences
        data object SignalingServers : WalletPreferences
        data object RelayServices : WalletPreferences
        data class DeveloperMode(val enabled: Boolean) : WalletPreferences
        data class CrashReporting(val enabled: Boolean) : WalletPreferences
        data class AdvancedLock(val enabled: Boolean) : WalletPreferences
        data class ThemePreference(val selection: ThemeSelection) : WalletPreferences

        @Composable
        fun title(): String {
            return when (this) {
                DepositGuarantees -> stringResource(id = R.string.preferences_depositGuarantees_title)
                Gateways -> stringResource(id = R.string.preferences_gateways)
                SignalingServers -> "Signaling Servers"
                RelayServices -> "Radix Connect Relay Services"
                is DeveloperMode -> stringResource(id = R.string.appSettings_developerMode_title)
                EntityHiding -> stringResource(id = R.string.preferences_hiddenEntities_title)
                AssetsHiding -> stringResource(id = R.string.preferences_hiddenAssets_title)
                is CrashReporting -> stringResource(id = R.string.appSettings_crashReporting_title)
                is AdvancedLock -> stringResource(id = R.string.preferences_advancedLock_title)
                is ThemePreference -> stringResource(id = R.string.preferences_themeSelection_title)
            }
        }

        @Composable
        fun subtitle(): String? {
            return when (this) {
                DepositGuarantees -> stringResource(id = R.string.preferences_depositGuarantees_subtitle)
                Gateways -> null
                SignalingServers -> "Manage signaling server and ICE profile options."
                RelayServices -> "Manage Radix Connect Relay services."
                is DeveloperMode -> stringResource(id = R.string.appSettings_developerMode_subtitle)
                EntityHiding -> stringResource(id = R.string.preferences_hiddenEntities_subtitle)
                AssetsHiding -> stringResource(id = R.string.preferences_hiddenAssets_subtitle)
                is CrashReporting -> null
                is AdvancedLock -> stringResource(id = R.string.preferences_advancedLockAndroid_subtitle)
                is ThemePreference -> stringResource(id = R.string.preferences_themeSelection_subtitle)
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                Gateways -> DSR.ic_gateways
                EntityHiding,
                AssetsHiding -> DSR.ic_entity_hiding

                DepositGuarantees -> DSR.ic_filter_list
                SignalingServers -> DSR.ic_sensors
                RelayServices -> DSR.ic_sensors
                is DeveloperMode -> DSR.ic_developer_mode
                is AdvancedLock -> DSR.ic_lock
                is ThemePreference -> DSR.ic_app_theme
                is CrashReporting -> null
            }
        }
    }

    sealed interface DebugSettingsItem {
        data object InspectProfile : DebugSettingsItem

        data object LinkConnectionStatusIndicator : DebugSettingsItem

        data object InspectCloudBackups : DebugSettingsItem

        data object SecurityFactorSamples : DebugSettingsItem

        data object ArculusTools : DebugSettingsItem

        @StringRes
        fun titleRes(): Int {
            return when (this) {
                InspectProfile -> R.string.settings_debugSettings_inspectProfile
                LinkConnectionStatusIndicator -> R.string.linkedConnectors_title
                InspectCloudBackups -> R.string.settings_debugSettings_inspectCloudBackups
                SecurityFactorSamples -> R.string.settings_debugSettings_securityFactorSamples
                ArculusTools -> R.string.settings_debugSettings_arculusTools
            }
        }

        @DrawableRes
        fun getIcon(): Int? { // add rest of icons
            return when (this) {
                InspectProfile -> com.babylon.wallet.android.designsystem.R.drawable.ic_personas
                LinkConnectionStatusIndicator -> com.babylon.wallet.android.designsystem.R.drawable.ic_desktop_connection
                InspectCloudBackups -> com.babylon.wallet.android.designsystem.R.drawable.ic_backup
                SecurityFactorSamples -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
                ArculusTools -> com.babylon.wallet.android.designsystem.R.drawable.ic_security
            }
        }

        companion object {
            fun values() = setOfNotNull(
                InspectProfile,
                LinkConnectionStatusIndicator,
                InspectCloudBackups,
                SecurityFactorSamples,
                ArculusTools.takeIf { Flavors.isFullVersion() }
            )
        }
    }
}
