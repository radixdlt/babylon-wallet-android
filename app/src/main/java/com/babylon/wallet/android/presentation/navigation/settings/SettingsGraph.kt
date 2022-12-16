package com.babylon.wallet.android.presentation.navigation.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingSectionItem
import com.babylon.wallet.android.presentation.settings.SettingsScreen
import com.babylon.wallet.android.presentation.settings.addconnection.SettingsAddConnectionScreen
import com.babylon.wallet.android.presentation.settings.editgateway.SettingsEditGatewayScreen
import com.babylon.wallet.android.presentation.settings.editgateway.SettingsEditGatewayViewModel
import com.google.accompanist.navigation.animation.composable

fun NavGraphBuilder.settingsNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = Screen.SettingsAllDestination.route, route = Screen.SettingsDestination.route
    ) {
        settingsAll(navController)
        settingsAddConnection(navController)
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
                    SettingSectionItem.AddConnection -> {
                        navController.navigate(Screen.SettingsAddConnectionDestination.route)
                    }
                    SettingSectionItem.DeleteAll -> {}
                    SettingSectionItem.EditGateway -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route)
                    }
                    SettingSectionItem.InspectProfile -> {}
                    SettingSectionItem.ManageConnections -> {}
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.settingsAddConnection(navController: NavController) {
    composable(
        route = Screen.SettingsAddConnectionDestination.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        SettingsAddConnectionScreen(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.popBackStack()
            },
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
        val vm: SettingsEditGatewayViewModel = hiltViewModel()
        SettingsEditGatewayScreen(
            viewModel = vm,
            onBackClick = {
                navController.popBackStack()
            },
            onCreateProfile = { url, networkName ->
                navController.navigate(
                    Screen.CreateAccountDestination.routeWithOptionalArgs(
                        Screen.ARG_NETWORK_URL to url,
                        Screen.ARG_NETWORK_NAME to networkName,
                        Screen.ARG_SWITCH_NETWORK to true
                    )
                )
            }
        )
    }
}
