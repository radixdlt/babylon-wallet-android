package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_SECURITY_CENTER_SCREEN = "settings_security_center_screen"

fun NavController.securityCenter() {
    navigate(ROUTE_SECURITY_CENTER_SCREEN)
}

fun NavGraphBuilder.securityCenter(
    navController: NavController
) {
    composable(
        route = ROUTE_SECURITY_CENTER_SCREEN,
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
        SecurityCenterScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}
