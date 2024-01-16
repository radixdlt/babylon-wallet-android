package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.appsettings.entityhiding.hiddenEntitiesScreen
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.GatewaysScreen
import com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors.linkedConnectorsScreen

const val ROUTE_APP_SETTINGS_SCREEN = "settings_app_settings_screen"
const val ROUTE_APP_SETTINGS_GRAPH = "settings_app_settings_graph"

fun NavController.appSettingsScreen() {
    navigate(ROUTE_APP_SETTINGS_SCREEN) {
        launchSingleTop = true
    }
}

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
        hiddenEntitiesScreen(onBackClick = {
            navController.popBackStack()
        })
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.appSettingsScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_APP_SETTINGS_SCREEN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
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
                    is SettingsItem.AppSettingsItem.CrashReporting,
                    is SettingsItem.AppSettingsItem.DeveloperMode -> {}
                    SettingsItem.AppSettingsItem.EntityHiding -> {
                        navController.hiddenEntitiesScreen()
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
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
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
