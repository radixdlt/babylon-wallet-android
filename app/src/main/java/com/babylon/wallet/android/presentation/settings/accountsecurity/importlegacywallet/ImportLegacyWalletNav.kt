package com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

private const val ROUTE = "import_legacy_wallet_route"

fun NavController.importLegacyWalletScreen() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.importLegacyWalletScreen(
    onBackClick: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        ImportLegacyWalletScreen(
            viewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onCloseScreen = onBackClick
        )
    }
}
