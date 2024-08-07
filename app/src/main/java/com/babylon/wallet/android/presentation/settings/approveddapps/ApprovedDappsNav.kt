package com.babylon.wallet.android.presentation.settings.approveddapps

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.AccountAddress

const val ROUTE_APPROVED_DAPPS = "settings_approved_dapps"

fun NavController.approvedDAppsScreen() {
    navigate(ROUTE_APPROVED_DAPPS) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.approvedDAppsScreen(
    onBackClick: () -> Unit,
    onDAppClick: (AccountAddress) -> Unit
) {
    composable(
        route = ROUTE_APPROVED_DAPPS,
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
        ApprovedDAppsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onDAppClick = onDAppClick
        )
    }
}
