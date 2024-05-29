package com.babylon.wallet.android.presentation.settings.debug.backups

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "inspect_google_backups"

fun NavController.inspectGoogleBackups() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.inspectGoogleBackups(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        InspectGoogleBackupsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
