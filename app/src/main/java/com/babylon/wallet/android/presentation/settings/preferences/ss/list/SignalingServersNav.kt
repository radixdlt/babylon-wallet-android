package com.babylon.wallet.android.presentation.settings.preferences.ss.list

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.P2pTransportProfile

private const val ROUTE = "signaling_servers_route"

fun NavController.signalingServers() {
    navigate(ROUTE)
}

fun NavGraphBuilder.signalingServers(
    onBackClick: () -> Unit,
    onServerClick: (P2pTransportProfile) -> Unit,
    onAddServerClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SignalingServersScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onServerClick = onServerClick,
            onAddServerClick = onAddServerClick
        )
    }
}
