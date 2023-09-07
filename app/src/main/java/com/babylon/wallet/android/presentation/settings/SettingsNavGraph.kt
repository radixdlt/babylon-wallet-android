package com.babylon.wallet.android.presentation.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.createpersona.personasScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityNavGraph
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityScreen
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsNavGraph
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.authorizedDAppsScreen
import com.babylon.wallet.android.presentation.settings.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.legacyimport.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation

@Suppress("LongMethod")
@OptIn(ExperimentalAnimationApi::class)
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
            }
        ) { personaAddress, requiredFields ->
            navController.personaEditScreen(personaAddress, requiredFields)
        }
        accountSecurityNavGraph(navController)
        appSettingsNavGraph(navController)
    }
}

@OptIn(ExperimentalAnimationApi::class)
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

                    SettingsItem.TopLevelSettings.Personas -> {
                        navController.personasScreen()
                    }

                    SettingsItem.TopLevelSettings.AccountSecurityAndSettings -> {
                        navController.accountSecurityScreen()
                    }

                    is SettingsItem.TopLevelSettings.AppSettings -> {
                        navController.appSettingsScreen()
                    }
                }
            }
        )
    }
}
