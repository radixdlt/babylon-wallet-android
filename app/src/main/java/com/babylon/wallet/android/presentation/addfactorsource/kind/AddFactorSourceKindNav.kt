package com.babylon.wallet.android.presentation.addfactorsource.kind

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_ADD_FACTOR_SOURCE_KIND = "add_factor_source_kind"

fun NavController.addFactorSourceKind() {
    navigate(ROUTE_ADD_FACTOR_SOURCE_KIND)
}

fun NavGraphBuilder.addFactorSourceKind(
    navController: NavController
) {
    composable(
        route = ROUTE_ADD_FACTOR_SOURCE_KIND,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        AddFactorSourceKindScreen(
            viewModel = hiltViewModel(),
            onDismiss = navController::popBackStack,
            onComplete = { navController.popBackStack() }
        )
    }
}
