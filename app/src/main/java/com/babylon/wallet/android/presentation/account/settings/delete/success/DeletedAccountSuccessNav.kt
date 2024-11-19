package com.babylon.wallet.android.presentation.account.settings.delete.success

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "deleted_account_success"

fun NavController.deletedAccountSuccess() {
    navigate(route = "$ROUTE")
}

fun NavGraphBuilder.deletedAccountSuccess(
    onGotoHomescreen: () -> Unit
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
        DeleteAccountSuccessScreen(
            onGotoHomescreen = onGotoHomescreen
        )
    }
}
