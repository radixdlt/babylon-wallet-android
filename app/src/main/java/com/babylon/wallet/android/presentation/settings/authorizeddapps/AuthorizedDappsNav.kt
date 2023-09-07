package com.babylon.wallet.android.presentation.settings.authorizeddapps

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.settings.authorizeddapps.dappdetail.ROUTE_DAPP_DETAIL
import com.google.accompanist.navigation.animation.composable

const val ROUTE_AUTHORIZED_DAPPS = "settings_authorized_dapps"

fun NavController.authorizedDAppsScreen() {
    navigate(ROUTE_AUTHORIZED_DAPPS) {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.authorizedDAppsScreen(
    onBackClick: () -> Unit,
    onDAppClick: (String) -> Unit
) {
    composable(
        route = ROUTE_AUTHORIZED_DAPPS,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_DAPP_DETAIL -> null
                else -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            when (initialState.destination.route) {
                ROUTE_DAPP_DETAIL -> null
                else -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
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
