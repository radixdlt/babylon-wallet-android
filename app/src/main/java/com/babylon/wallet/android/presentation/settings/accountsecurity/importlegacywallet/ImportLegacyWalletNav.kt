package com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE = "import_legacy_wallet_route"

fun NavController.importLegacyWalletScreen() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.importLegacyWalletScreen(
    onBackClick: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        ImportLegacyWalletScreen(
            viewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onCloseScreen = onBackClick
        )
    }
}
