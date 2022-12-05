package com.babylon.wallet.android.presentation.navigation.settings

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingSectionItem
import com.babylon.wallet.android.presentation.settings.SettingsScreen
import com.babylon.wallet.android.presentation.settings.addconnection.SettingsAddConnectionScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = Screen.SettingsAllDestination.route,
        route = Screen.SettingsDestination.route
    ) {
        composable(route = Screen.SettingsAllDestination.route) {
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
                        SettingSectionItem.EditGateway -> {}
                        SettingSectionItem.InspectProfile -> {}
                        SettingSectionItem.ManageConnections -> {}
                    }
                }
            )
        }
        composable(route = Screen.SettingsAddConnectionDestination.route) {
            SettingsAddConnectionScreen(
                viewModel = hiltViewModel(),
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}
