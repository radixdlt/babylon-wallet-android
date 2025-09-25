package com.babylon.wallet.android.presentation.settings.debug.arculus

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_ARCULUS_TOOLS = "debug_arculus_tools"

fun NavController.arculusTools() {
    navigate(ROUTE_ARCULUS_TOOLS)
}

fun NavGraphBuilder.arculusTools(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_ARCULUS_TOOLS,
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
        ArculusToolsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
