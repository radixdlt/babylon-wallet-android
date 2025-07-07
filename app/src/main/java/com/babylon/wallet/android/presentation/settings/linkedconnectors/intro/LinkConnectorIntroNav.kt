package com.babylon.wallet.android.presentation.settings.linkedconnectors.intro

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val LINK_CONNECTOR_INTR_ROUTE = "link_connector_intro_route"

fun NavController.linkConnectorIntro() {
    navigate(route = LINK_CONNECTOR_INTR_ROUTE)
}

fun NavGraphBuilder.linkConnectorIntro(
    onDismiss: () -> Unit,
    onLinkConnectorClick: () -> Unit
) {
    composable(
        route = LINK_CONNECTOR_INTR_ROUTE,
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
        LinkConnectorIntroScreen(
            onLinkConnectorClick = onLinkConnectorClick,
            onDismiss = onDismiss
        )
    }
}
