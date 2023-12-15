package com.babylon.wallet.android.presentation.account.recover.complete

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "recovery_scan_complete"

fun NavController.recoveryScanComplete() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.recoveryScanComplete(
    onContinueClick: () -> Unit
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
        RecoveryScanCompleteScreen(
            onContinueClick = onContinueClick
        )
    }
}
