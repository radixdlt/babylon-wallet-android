package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_APPLY_SHIELD = "apply_shield"

fun NavController.applyShield() {
    navigate(ROUTE_APPLY_SHIELD)
}

fun NavGraphBuilder.applyShield(
    navController: NavController
) {
    composable(
        route = ROUTE_APPLY_SHIELD,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        ApplyShieldScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() }
        )
    }
}
