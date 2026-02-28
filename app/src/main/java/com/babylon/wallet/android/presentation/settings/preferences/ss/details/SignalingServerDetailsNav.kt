package com.babylon.wallet.android.presentation.settings.preferences.ss.details

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ARG_ID = "arg_id"

private const val DESTINATION = "signaling_server_details_route"
private const val ROUTE = "$DESTINATION?$ARG_ID={$ARG_ID}"

class SignalingServerDetailsNavArgs(
    val id: String?
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        savedStateHandle.get<String?>(ARG_ID).takeIf { it != "null" }
    )
}

fun NavController.signalingServerDetails(id: String?) {
    navigate("$DESTINATION?$ARG_ID=$id")
}

fun NavGraphBuilder.signalingServerDetails(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SignalingServerDetailsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
