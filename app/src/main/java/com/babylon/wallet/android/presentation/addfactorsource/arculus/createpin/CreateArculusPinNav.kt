package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.name.setFactorSourceName

private const val ROUTE_CREATE_ARCULUS_PIN = "create_arculus_pin"

fun NavController.createArculusPin() {
    navigate(ROUTE_CREATE_ARCULUS_PIN)
}

fun NavGraphBuilder.createArculusPin(
    navController: NavController
) {
    composable(
        route = ROUTE_CREATE_ARCULUS_PIN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        CreateArculusPinScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onConfirmed = { navController.setFactorSourceName() }
        )
    }
}
