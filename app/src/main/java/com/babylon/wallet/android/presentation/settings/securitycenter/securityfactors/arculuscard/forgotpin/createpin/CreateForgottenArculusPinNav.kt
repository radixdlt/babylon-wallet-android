package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.forgotpin.createpin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin.CreateArculusPinScreen

private const val ROUTE_CREATE_FORGOTTEN_ARCULUS_PIN = "create_forgotten_arculus_pin"

fun NavController.createForgottenArculusPin() {
    navigate(ROUTE_CREATE_FORGOTTEN_ARCULUS_PIN)
}

fun NavGraphBuilder.createForgottenArculusPin(
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    composable(
        route = ROUTE_CREATE_FORGOTTEN_ARCULUS_PIN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        CreateArculusPinScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onConfirmed = onConfirmed
        )
    }
}
