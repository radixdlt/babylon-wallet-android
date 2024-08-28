package com.babylon.wallet.android.presentation.settings.preferences.assetshiding

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "assets_hiding_route"

fun NavController.hiddenAssetsScreen() {
    navigate(ROUTE)
}

fun NavGraphBuilder.hiddenAssetsScreen(
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
        HiddenAssetsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
