package com.babylon.wallet.android.presentation.settings.linkedconnectors.relink

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "relink_connectors_route"

fun NavController.relinkConnectors() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.relinkConnectors(
    onContinueClick: () -> Unit,
    onDismiss: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        RelinkConnectorsScreen(
            onContinueClick = onContinueClick,
            onDismiss = onDismiss
        )
    }
}
