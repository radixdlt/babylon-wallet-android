package com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scancomplete

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        RecoveryScanCompleteScreen(
            onContinueClick = onContinueClick
        )
    }
}
