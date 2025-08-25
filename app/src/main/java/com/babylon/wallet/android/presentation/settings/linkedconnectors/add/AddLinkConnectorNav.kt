package com.babylon.wallet.android.presentation.settings.linkedconnectors.add

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

private const val ROUTE = "add_link_connector_route"

fun NavController.addLinkConnector() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.addLinkConnector(
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
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
        AddLinkConnectorScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onInfoClick = onInfoClick
        )
    }
}
