package com.babylon.wallet.android.presentation.p2plinksmigration.upgrade

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "linked_connectors_upgrade_route"

fun NavController.linkedConnectorsUpgrade() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.linkedConnectorsUpgrade(
    onContinueClick: () -> Unit,
    onDismiss: () -> Unit
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
        LinkedConnectorsUpgradeScreen(
            onContinueClick = onContinueClick,
            onDismiss = onDismiss
        )
    }
}
