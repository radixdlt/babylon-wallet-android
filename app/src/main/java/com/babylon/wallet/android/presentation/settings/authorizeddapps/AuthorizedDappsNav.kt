package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.ROUTE_DAPP_DETAIL

const val ROUTE_AUTHORIZED_DAPPS = "settings_authorized_dapps"

fun NavController.authorizedDAppsScreen() {
    navigate(ROUTE_AUTHORIZED_DAPPS) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.authorizedDAppsScreen(
    onBackClick: () -> Unit,
    onDAppClick: (String) -> Unit
) {
    composable(
        route = ROUTE_AUTHORIZED_DAPPS,
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
        AuthorizedDAppsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onDAppClick = onDAppClick
        )
    }
}
