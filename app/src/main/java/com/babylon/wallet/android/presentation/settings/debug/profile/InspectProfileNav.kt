package com.babylon.wallet.android.presentation.settings.debug.profile

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "inspect_profile"

fun NavController.inspectProfile() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.inspectProfile(
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
        InspectProfileScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
