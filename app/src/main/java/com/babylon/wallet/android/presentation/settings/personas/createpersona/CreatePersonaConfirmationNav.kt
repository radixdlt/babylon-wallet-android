package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

@VisibleForTesting
private const val ROUTE = "persona_completion_route"

fun NavController.createPersonaConfirmationScreen() {
    navigate("persona_completion_route")
}

fun NavGraphBuilder.createPersonaConfirmationScreen(
    finishPersonaCreation: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        CreatePersonaConfirmationScreen(
            viewModel = hiltViewModel(),
            finishPersonaCreation = finishPersonaCreation
        )
    }
}
