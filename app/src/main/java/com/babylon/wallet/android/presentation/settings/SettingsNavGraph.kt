package com.babylon.wallet.android.presentation.navigation.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.personaScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsScreen
import com.babylon.wallet.android.presentation.settings.appsettings.appSettingsScreen
import com.babylon.wallet.android.presentation.settings.authorizeddapps.authorizedDappsScreen
import com.babylon.wallet.android.presentation.settings.connector.settingsConnectorScreen
import com.babylon.wallet.android.presentation.settings.dappdetail.dappDetailScreen
import com.babylon.wallet.android.presentation.settings.editgateway.SettingsEditGatewayScreen
import com.babylon.wallet.android.presentation.settings.legacyimport.settingsImportOlympiaAccounts
import com.babylon.wallet.android.presentation.settings.personaedit.personaEditScreen
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation

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
        authorizedDappsScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onDappClick = { dappDefinitionAddress ->
                navController.dappDetailScreen(dappDefinitionAddress)
            }
        )
        dappDetailScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onEditPersona = { personaAddress, requiredFields ->
                navController.personaEditScreen(personaAddress, requiredFields)
            }
        )
        settingsGatewayEdit(navController)
        settingsImportOlympiaAccounts(
            onBackClick = {
                navController.popBackStack()
            }
        )
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
                navController.popBackStack()
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
                        navController.authorizedDappsScreen()
                    }
                    SettingsItem.TopLevelSettings.AppSettings -> {
                        navController.appSettingsScreen()
                    }
                    SettingsItem.TopLevelSettings.ImportFromLegacyWallet -> {
                        navController.settingsImportOlympiaAccounts()
                    }
                    else -> {}
                }
            },
            onProfileDeleted = {
                navController.popBackStack(Screen.WalletDestination.route, false)
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
