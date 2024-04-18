package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_TROUBLESHOOTING_SCREEN = "settings_troubleshooting_screen"

fun NavController.troubleshootingSettings() {
    navigate(ROUTE_TROUBLESHOOTING_SCREEN)
}

fun NavGraphBuilder.troubleshootingSettings(
    navController: NavController
) {
    composable(
        route = ROUTE_TROUBLESHOOTING_SCREEN,
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
        TroubleshootingSettingsScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
