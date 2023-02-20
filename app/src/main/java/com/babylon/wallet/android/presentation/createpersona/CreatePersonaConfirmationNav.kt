package com.babylon.wallet.android.presentation.createpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
private const val ROUTE_PERSONA_COMPLETION = "persona_completion_route"
internal const val ARG_PERSONA_ID = "arg_persona_id"

internal class CreatePersonaConfirmationArgs(val personaId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ID]) as String
    )
}

fun NavController.createPersonaConfirmationScreen(personaId: String) {
    navigate("$ROUTE_PERSONA_COMPLETION/$personaId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createPersonaConfirmationScreen(
    finishPersonaCreation: () -> Unit
) {
    composable(
        route = "$ROUTE_PERSONA_COMPLETION/{$ARG_PERSONA_ID}",
        arguments = listOf(
            navArgument(ARG_PERSONA_ID) { type = NavType.StringType },
        )
    ) {
        CreatePersonaConfirmationScreen(
            viewModel = hiltViewModel(),
            finishPersonaCreation = finishPersonaCreation
        )
    }
}
