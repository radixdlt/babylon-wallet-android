package com.babylon.wallet.android.presentation.settings.preferences.ss

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "signalingServers_route"

fun NavController.signalingServers() {
    navigate(ROUTE)
}

fun NavGraphBuilder.signalingServers(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SignalingServersScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
