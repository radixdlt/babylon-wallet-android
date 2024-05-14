package com.babylon.wallet.android.presentation.settings.troubleshooting.reset

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavController.resetWalletScreen() {
    navigate("troubleshooting_reset_wallet")
}

fun NavGraphBuilder.resetWalletScreen(
    onProfileDeleted: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = "troubleshooting_reset_wallet",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        FactoryResetScreen(
            viewModel = hiltViewModel(),
            onProfileDeleted = onProfileDeleted,
            onBackClick = onBackClick
        )
    }
}
