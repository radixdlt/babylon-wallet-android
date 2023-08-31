package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

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

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.linkedConnectorsScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = "$ROUTE/{$ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN}",
        arguments = listOf(
            navArgument(ARG_SHOW_ADD_LINK_CONNECTOR_SCREEN) { type = NavType.BoolType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        LinkedConnectorsScreen(
            viewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
