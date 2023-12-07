package com.babylon.wallet.android.presentation.settings

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityNavGraph
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsNavGraph
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsScreen
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.authorizedDAppsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.debug.debugSettings
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personasScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.status.assets.fungible.fungibleAssetDialog
import com.babylon.wallet.android.presentation.status.assets.nonfungible.nonFungibleAssetDialog

@Suppress("LongMethod")
fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = Screen.SettingsAllDestination.route,
        route = Screen.SettingsDestination.route
    ) {
        settingsAll(navController)
        authorizedDAppsScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onDAppClick = { dAppDefinitionAddress ->
                navController.dAppDetailScreen(dAppDefinitionAddress)
            }
        )
        dAppDetailScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onEditPersona = { personaAddress, requiredFields ->
                navController.personaEditScreen(personaAddress, requiredFields)
            },
            onFungibleClick = { resource ->
                navController.fungibleAssetDialog(resourceAddress = resource.resourceAddress)
            },
            onNonFungibleClick = { resource ->
                navController.nonFungibleAssetDialog(resourceAddress = resource.resourceAddress)
            }
        )
        accountSecurityNavGraph(navController)
        appSettingsNavGraph(navController)
        debugSettings(navController)
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

                    SettingsItem.TopLevelSettings.ImportOlympiaWallet -> {
                        navController.importLegacyWalletScreen()
                    }

                    SettingsItem.TopLevelSettings.AuthorizedDapps -> {
                        navController.authorizedDAppsScreen()
                    }

                    is SettingsItem.TopLevelSettings.Personas -> {
                        navController.personasScreen()
                    }

                    is SettingsItem.TopLevelSettings.AccountSecurityAndSettings -> {
                        navController.accountSecurityScreen()
                    }

                    is SettingsItem.TopLevelSettings.AppSettings -> {
                        navController.appSettingsScreen()
                    }

                    is SettingsItem.TopLevelSettings.DebugSettings -> {
                        navController.debugSettings()
                    }
                }
            }
        )
    }
}
