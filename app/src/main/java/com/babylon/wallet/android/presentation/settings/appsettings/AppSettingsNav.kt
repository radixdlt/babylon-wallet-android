package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.linkedconnectors.linkedConnectorsScreen
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
                }
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
