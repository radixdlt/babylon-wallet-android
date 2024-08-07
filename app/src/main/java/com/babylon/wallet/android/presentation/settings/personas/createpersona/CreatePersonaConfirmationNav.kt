package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

@VisibleForTesting
private const val ROUTE = "persona_completion_route/{$ARG_REQUEST_SOURCE}"

fun NavController.createPersonaConfirmationScreen(requestSource: CreatePersonaRequestSource) {
    navigate("persona_completion_route/$requestSource")
}

fun NavGraphBuilder.createPersonaConfirmationScreen(
    finishPersonaCreation: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(
                    CreatePersonaRequestSource::class.java
                )
            }
        ),
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
        val requestSource = it.getCreatePersonaRequestSource()
        CreatePersonaConfirmationScreen(
            viewModel = hiltViewModel(),
            finishPersonaCreation = finishPersonaCreation,
            requestSource = requestSource
        )
    }
}
