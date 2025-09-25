package com.babylon.wallet.android.presentation.settings.debug.backups

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        InspectGoogleBackupsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
