package com.babylon.wallet.android.presentation.settings

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.BuildConfig
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.approveddapps.approvedDAppsScreen
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.debug.debugSettings
import com.babylon.wallet.android.presentation.settings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personasScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.preferences.preferencesNavGraph
import com.babylon.wallet.android.presentation.settings.preferences.walletPreferencesScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityCenter
import com.babylon.wallet.android.presentation.settings.securitycenter.securityCenterNavGraph
import com.babylon.wallet.android.presentation.settings.troubleshooting.troubleshootingNavGraph
import com.babylon.wallet.android.presentation.settings.troubleshooting.troubleshootingSettings
import com.babylon.wallet.android.presentation.status.assets.fungibleAssetDialog
import com.babylon.wallet.android.presentation.status.assets.nftAssetDialog

@Suppress("LongMethod")
fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = Screen.SettingsAllDestination.route,
        route = Screen.SettingsDestination.route
    ) {
        settingsAll(navController)
        approvedDAppsScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onDAppClick = { dAppDefinitionAddress ->
                navController.dAppDetailScreen(dAppDefinitionAddress)
            }
        )
        linkedConnectorsScreen(onBackClick = {
            navController.popBackStack()
        })
        dAppDetailScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onEditPersona = { personaAddress, requiredFields ->
                navController.personaEditScreen(personaAddress, requiredFields)
            },
            onFungibleClick = { resource ->
                navController.fungibleAssetDialog(resourceAddress = resource.address)
            },
            onNonFungibleClick = { resource ->
                navController.nftAssetDialog(resourceAddress = resource.address)
            }
        )
        preferencesNavGraph(navController)
        securityCenterNavGraph(navController)
        troubleshootingNavGraph(navController)
        if (BuildConfig.EXPERIMENTAL_FEATURES_ENABLED) {
            debugSettings(navController)
        }
    }
}

private fun NavGraphBuilder.settingsAll(navController: NavController) {
    composable(
        route = Screen.SettingsAllDestination.route
    ) {
        SettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.navigateUp()
            },
            onSettingClick = { item ->
                when (item) {
                    SettingsItem.TopLevelSettings.LinkToConnector -> {
                        navController.linkedConnectorsScreen(shouldShowAddLinkConnectorScreen = true)
                    }

                    SettingsItem.TopLevelSettings.ApprovedDapps -> {
                        navController.approvedDAppsScreen()
                    }

                    is SettingsItem.TopLevelSettings.Personas -> {
                        navController.personasScreen()
                    }

                    is SettingsItem.TopLevelSettings.Preferences -> {
                        navController.walletPreferencesScreen()
                    }

                    is SettingsItem.TopLevelSettings.DebugSettings -> {
                        navController.debugSettings()
                    }

                    SettingsItem.TopLevelSettings.LinkedConnectors -> {
                        navController.linkedConnectorsScreen()
                    }

                    SettingsItem.TopLevelSettings.SecurityCenter -> {
                        navController.securityCenter()
                    }
                    SettingsItem.TopLevelSettings.Troubleshooting -> {
                        navController.troubleshootingSettings()
                    }
                }
            }
        )
    }
}
