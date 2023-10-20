package com.babylon.wallet.android.presentation.settings.appsettings.entityhiding

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "entity_hiding_route"

fun NavController.hiddenEntitiesScreen() {
    navigate(ROUTE)
}

fun NavGraphBuilder.hiddenEntitiesScreen(
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
        HiddenEntitiesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
