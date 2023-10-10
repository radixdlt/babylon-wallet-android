package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.appsettings.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.appsettings.backup.systemBackupSettingsScreen
import com.babylon.wallet.android.presentation.settings.appsettings.entityhiding.entityHidingScreen
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.GatewaysScreen
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.linkedConnectorsScreen
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation

const val ROUTE_APP_SETTINGS_SCREEN = "settings_app_settings_screen"
const val ROUTE_APP_SETTINGS_GRAPH = "settings_app_settings_graph"

fun NavController.appSettingsScreen() {
    navigate(ROUTE_APP_SETTINGS_SCREEN) {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.appSettingsNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = ROUTE_APP_SETTINGS_SCREEN,
        route = ROUTE_APP_SETTINGS_GRAPH
    ) {
        appSettingsScreen(navController)
        linkedConnectorsScreen(onBackClick = {
            navController.popBackStack()
        })
        settingsGateway(navController)
        entityHidingScreen(onBackClick = {
            navController.popBackStack()
        })
        backupScreen(
            onSystemBackupSettingsClick = {
                navController.systemBackupSettingsScreen()
            },
            onProfileDeleted = {
                navController.popBackStack(MAIN_ROUTE, false)
            },
            onClose = {
                navController.popBackStack()
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.appSettingsScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_APP_SETTINGS_SCREEN,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        AppSettingsScreen(
            viewModel = hiltViewModel(),
            onAppSettingItemClick = { appSettingsItem ->
                when (appSettingsItem) {
                    SettingsItem.AppSettingsItem.LinkedConnectors -> {
                        navController.linkedConnectorsScreen()
                    }

                    SettingsItem.AppSettingsItem.Gateways -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route)
                    }

                    is SettingsItem.AppSettingsItem.Backups -> {
                        navController.backupScreen()
                    }

                    is SettingsItem.AppSettingsItem.DeveloperMode -> {}
                    SettingsItem.AppSettingsItem.EntityHiding -> {
                        navController.entityHidingScreen()
                    }
                }
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.settingsGateway(navController: NavController) {
    composable(
        route = Screen.SettingsEditGatewayApiDestination.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        GatewaysScreen(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.popBackStack()
            },
            onCreateProfile = { url, networkId ->
                navController.createAccountScreen(
                    CreateAccountRequestSource.Gateways,
                    url,
                    networkId,
                    true
                )
            }
        )
    }
}
