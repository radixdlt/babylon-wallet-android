package com.babylon.wallet.android.presentation.settings.linkedconnectors

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

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
    shouldShowAddLinkConnectorScreen: Boolean = false,
    popUpToRoute: String? = null,
    popUpInclusive: Boolean = true
) {
    navigate("$ROUTE/$shouldShowAddLinkConnectorScreen") {
        launchSingleTop = true
        popUpToRoute?.let {
            popBackStack(it, popUpInclusive)
        }
    }
}

fun NavGraphBuilder.linkedConnectorsScreen(
    onInfoClick: (GlossaryItem) -> Unit,
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
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        LinkedConnectorsScreen(
            viewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onInfoClick = onInfoClick,
            onBackClick = onBackClick
        )
    }
}
