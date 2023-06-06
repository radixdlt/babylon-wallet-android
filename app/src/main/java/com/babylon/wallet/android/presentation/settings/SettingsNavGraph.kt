package com.babylon.wallet.android.presentation.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.personaScreen
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.authorizedDAppsScreen
import com.babylon.wallet.android.presentation.settings.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.backup.restoreMnemonic
import com.babylon.wallet.android.presentation.settings.backup.systemBackupSettingsScreen
import com.babylon.wallet.android.presentation.settings.connector.settingsConnectorScreen
import com.babylon.wallet.android.presentation.settings.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.editgateway.SettingsEditGatewayScreen
import com.babylon.wallet.android.presentation.settings.ledgerfactorsource.settingsLedgerFactorSourcesScreen
import com.babylon.wallet.android.presentation.settings.legacyimport.settingsImportOlympiaAccounts
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.babylon.wallet.android.presentation.settings.seedphrase.settingsShowMnemonic
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
        settingsConnectorScreen(onBackClick = {
            navController.popBackStack()
        })
        appSettingsScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        backupScreen(
            onSystemBackupSettingsClick = {
                navController.systemBackupSettingsScreen()
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
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
        settingsGatewayEdit(navController)
        settingsShowMnemonic(
            onBackClick = { navController.popBackStack() },
            onNavigateToRecoverMnemonic = { navController.restoreMnemonic(it) }
        )
        settingsImportOlympiaAccounts(
            onBackClick = {
                navController.popBackStack()
            },
            onAddP2PLink = {
                navController.settingsConnectorScreen(scanQr = true)
            }
        )
        settingsLedgerFactorSourcesScreen(
            onBackClick = {
                navController.navigateUp()
            }
        ) {
            navController.settingsConnectorScreen(scanQr = true)
        }
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
                    SettingsItem.TopLevelSettings.Connection -> {
                        navController.settingsConnectorScreen(scanQr = true)
                    }
                    SettingsItem.TopLevelSettings.Gateways -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route)
                    }
                    SettingsItem.TopLevelSettings.Personas -> {
                        navController.personaScreen()
                    }
                    SettingsItem.TopLevelSettings.LinkedConnector -> {
                        navController.settingsConnectorScreen()
                    }
                    SettingsItem.TopLevelSettings.AuthorizedDapps -> {
                        navController.authorizedDAppsScreen()
                    }
                    SettingsItem.TopLevelSettings.AppSettings -> {
                        navController.appSettingsScreen()
                    }
                    SettingsItem.TopLevelSettings.ShowMnemonic -> {
                        navController.settingsShowMnemonic()
                    }
                    SettingsItem.TopLevelSettings.ImportFromLegacyWallet -> {
                        navController.settingsImportOlympiaAccounts()
                    }
                    is SettingsItem.TopLevelSettings.Backups -> {
                        navController.backupScreen()
                    }
                    SettingsItem.TopLevelSettings.LedgerHardwareWallets -> {
                        navController.settingsLedgerFactorSourcesScreen()
                    }
                    else -> {}
                }
            },
            onProfileDeleted = {
                navController.popBackStack(MAIN_ROUTE, false)
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.settingsGatewayEdit(navController: NavController) {
    composable(
        route = Screen.SettingsEditGatewayApiDestination.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        SettingsEditGatewayScreen(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.popBackStack()
            },
            onCreateProfile = { url, networkName ->
                navController.createAccountScreen(
                    CreateAccountRequestSource.Gateways,
                    url,
                    networkName,
                    true
                )
            }
        )
    }
}
