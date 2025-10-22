package com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val ARG_CONTEXT = "context"
private const val PATH_CREATE_ARCULUS_PIN = "create_arculus_pin"
private const val ROUTE_CREATE_ARCULUS_PIN = "$PATH_CREATE_ARCULUS_PIN/{$ARG_CONTEXT}"

internal class CreateArculusPinArgs(val context: CreateArculusPinContext) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<CreateArculusPinContext>(ARG_CONTEXT))
    )
}

fun NavController.createArculusPin(context: CreateArculusPinContext) {
    navigate("$PATH_CREATE_ARCULUS_PIN/$context")
}

fun NavGraphBuilder.createArculusPin(
    onDismiss: () -> Unit,
    onConfirmed: (CreateArculusPinContext) -> Unit
) {
    composable(
        route = ROUTE_CREATE_ARCULUS_PIN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) },
        arguments = listOf(
            navArgument(ARG_CONTEXT) {
                type = NavType.EnumType(CreateArculusPinContext::class.java)
            }
        )
    ) {
        CreateArculusPinScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onConfirmed = onConfirmed
        )
    }
}
