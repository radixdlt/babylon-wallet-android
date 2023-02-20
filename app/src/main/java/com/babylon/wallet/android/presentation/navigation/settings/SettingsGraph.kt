package com.babylon.wallet.android.presentation.navigation.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.personaScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingSectionItem
import com.babylon.wallet.android.presentation.settings.SettingsScreen
import com.babylon.wallet.android.presentation.settings.addconnection.settingsConnectionScreen
import com.babylon.wallet.android.presentation.settings.connecteddapps.connectedDappsScreen
import com.babylon.wallet.android.presentation.settings.dappdetail.dappDetailScreen
import com.babylon.wallet.android.presentation.settings.editgateway.SettingsEditGatewayScreen
import com.google.accompanist.navigation.animation.composable

fun NavGraphBuilder.settingsNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = Screen.SettingsAllDestination.route,
        route = Screen.SettingsDestination.route
    ) {
        settingsAll(navController)
        settingsConnectionScreen(onBackClick = {
            navController.popBackStack()
        })
        connectedDappsScreen(
            onBackClick = {
                navController.popBackStack()
            },
            onDappClick = { dappDefinitionAddress ->
                navController.dappDetailScreen(dappDefinitionAddress)
            }
        )
        dappDetailScreen(onBackClick = {
            navController.popBackStack()
        })
        settingsGatewayEdit(navController)
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
                    SettingSectionItem.Connection -> {
                        navController.settingsConnectionScreen(scanQr = true)
                    }
                    SettingSectionItem.DeleteAll -> {}
                    SettingSectionItem.Gateway -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route)
                    }
                    SettingSectionItem.Personas -> {
                        navController.personaScreen()
                    }
                    SettingSectionItem.InspectProfile -> {}
                    SettingSectionItem.LinkedConnector -> {
                        navController.settingsConnectionScreen()
                    }
                    SettingSectionItem.ConnectedDapps -> {
                        navController.connectedDappsScreen()
                    }
                    else -> {}
                }
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
                navController.createAccountScreen(CreateAccountRequestSource.Settings, url, networkName, true)
            }
        )
    }
}
