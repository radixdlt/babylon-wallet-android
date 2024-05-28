package com.babylon.wallet.android.presentation.settings.debug

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.SettingsItem.DebugSettingsItem.InspectProfile
import com.babylon.wallet.android.presentation.settings.debug.profile.inspectProfile

const val ROUTE_DEBUG_SETTINGS_SCREEN = "settings_debug_settings_screen"
const val ROUTE_DEBUG_SETTINGS_GRAPH = "settings_debug_settings_graph"

fun NavController.debugSettings() {
    navigate(ROUTE_DEBUG_SETTINGS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.debugSettings(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_DEBUG_SETTINGS_SCREEN,
        route = ROUTE_DEBUG_SETTINGS_GRAPH
    ) {
        composable(
            route = ROUTE_DEBUG_SETTINGS_SCREEN,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            },
            exitTransition = {
                null
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
            popEnterTransition = {
                EnterTransition.None
            }
        ) {
            DebugSettingsScreen(
                viewModel = hiltViewModel(),
                onBackClick = {
                    navController.popBackStack()
                },
                onItemClick = { item ->
                    when (item) {
                        InspectProfile -> navController.inspectProfile()
                        SettingsItem.DebugSettingsItem.LinkConnectionStatusIndicator -> {}
                        SettingsItem.DebugSettingsItem.MobileConnectDelay -> {}
                    }
                }
            )
        }
        inspectProfile(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
