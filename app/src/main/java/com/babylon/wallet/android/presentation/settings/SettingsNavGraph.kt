package com.babylon.wallet.android.presentation.settings

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityNavGraph
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountSecurityScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsNavGraph
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.settings.account.specificassets.specificAssets
import com.babylon.wallet.android.presentation.settings.account.specificdepositor.specificDepositor
import com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits.accountThirdPartyDeposits
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsScreen
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.linkedConnectorsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.authorizedDAppsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.dAppDetailScreen
import com.babylon.wallet.android.presentation.settings.personas.createpersona.personasScreen
import com.babylon.wallet.android.presentation.settings.personas.personaedit.personaEditScreen
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
        settingsGatewayEdit(navController)
        seedPhrases(
            onBackClick = { navController.popBackStack() },
            onNavigateToRecoverMnemonic = { navController.restoreMnemonics(args = RestoreMnemonicsArgs.RestoreSpecificMnemonic(it.body)) },
            onNavigateToSeedPhrase = { navController.revealSeedPhrase(it.body.value) }
        )
        importLegacyWalletScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        ledgerHardwareWalletsScreen(
            onBackClick = {
                navController.navigateUp()
            }
        )
        revealSeedPhrase(
            onBackClick = {
                navController.navigateUp()
            }
        )
        accountThirdPartyDeposits(
            navController = navController,
            onBackClick = {
                navController.navigateUp()
            },
            onAssetSpecificRulesClick = {
                navController.specificAssets(it)
            },
            onSpecificDepositorsClick = {
                navController.specificDepositor()
            }
        )
        specificAssets(navController = navController, onBackClick = {
            navController.navigateUp()
        })
        specificDepositor(navController = navController, onBackClick = {
            navController.navigateUp()
        })
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
                    SettingsItem.TopLevelSettings.LinkedConnectors -> {
                        navController.linkedConnectorsScreen()
                    }

                    SettingsItem.TopLevelSettings.Gateways -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route) {
                            launchSingleTop = true
                        }
                    }

                    SettingsItem.TopLevelSettings.Personas -> {
                        navController.personaScreen()
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

                    SettingsItem.TopLevelSettings.AppSettings -> {
                        navController.appSettingsScreen()
                    }

                    SettingsItem.TopLevelSettings.SeedPhrases -> {
                        navController.seedPhrases()
                    }

                    is SettingsItem.TopLevelSettings.Backups -> {
                        navController.backupScreen()
                    }

                    SettingsItem.TopLevelSettings.LedgerHardwareWallets -> {
                        navController.ledgerHardwareWalletsScreen()
                    }

                    SettingsItem.TopLevelSettings.ImportFromLegacyWallet -> {
                        navController.importLegacyWalletScreen()
                    }
                }
            }
        )
    }
}
