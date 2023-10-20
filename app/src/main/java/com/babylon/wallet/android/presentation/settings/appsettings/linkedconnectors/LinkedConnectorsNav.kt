package com.babylon.wallet.android.presentation.settings.appsettings.linkedconnectors

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@VisibleForTesting
internal const val ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN = "arg_show_add_link_connector_screen"

private const val ROUTE = "linked_connectors_route"

internal class LinkedConnectorsScreenArgs(
    val shouldShowAddLinkConnectorScreen: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN]) as Boolean
    )
}

fun NavController.linkedConnectorsScreen(
    shouldShowAddLinkConnectorScreen: Boolean = false
) {
    navigate("$ROUTE/$shouldShowAddLinkConnectorScreen") {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.linkedConnectorsScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = "$ROUTE/{$ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN}",
        arguments = listOf(
            navArgument(ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN) { type = NavType.BoolType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        LinkedConnectorsScreen(
            viewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
