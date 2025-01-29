package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_CHOOSE_PERSONAS = "choose_personas"

fun NavController.choosePersonas() {
    navigate(ROUTE_CHOOSE_PERSONAS)
}

fun NavGraphBuilder.choosePersonas(
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_PERSONAS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        ChoosePersonasScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() }
        )
    }
}
