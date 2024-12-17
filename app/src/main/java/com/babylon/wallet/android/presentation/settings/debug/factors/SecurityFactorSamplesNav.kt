package com.babylon.wallet.android.presentation.settings.debug.factors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "security_factor_samples"

fun NavController.securityFactorSamples() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.securityFactorSamples(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        SecurityFactorSamplesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
