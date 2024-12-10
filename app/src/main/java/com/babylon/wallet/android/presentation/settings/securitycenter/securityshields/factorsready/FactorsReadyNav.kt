package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.factorsready

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.ROUTE_SECURITY_SHIELDS_GRAPH

const val ROUTE_FACTORS_READY = "factors_ready"

fun NavController.factorsReadyScreen() {
    navigate(ROUTE_FACTORS_READY)
}

fun NavGraphBuilder.factorsReadyScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_FACTORS_READY,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        FactorsReadyScreen(
            onDismiss = { navController.popBackStack() },
            onBuildShieldClick = { navController.popBackStack(ROUTE_SECURITY_SHIELDS_GRAPH, true) }
        )
    }
}
