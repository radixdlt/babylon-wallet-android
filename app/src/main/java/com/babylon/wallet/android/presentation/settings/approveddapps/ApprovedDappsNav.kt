package com.babylon.wallet.android.presentation.settings.approveddapps

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.approveddapps.dappdetail.ROUTE_DAPP_DETAIL
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
            when (targetState.destination.route) {
                ROUTE_DAPP_DETAIL -> null
                else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            when (initialState.destination.route) {
                ROUTE_DAPP_DETAIL -> null
                else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        ApprovedDAppsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onDAppClick = onDAppClick
        )
    }
}
