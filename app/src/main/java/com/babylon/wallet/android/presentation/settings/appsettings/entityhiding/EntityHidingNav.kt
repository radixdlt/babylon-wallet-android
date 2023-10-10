package com.babylon.wallet.android.presentation.settings.appsettings.entityhiding

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

private const val ROUTE = "entity_hiding_route"

fun NavController.entityHidingScreen() {
    navigate(ROUTE)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.entityHidingScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        EntityHidingScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
