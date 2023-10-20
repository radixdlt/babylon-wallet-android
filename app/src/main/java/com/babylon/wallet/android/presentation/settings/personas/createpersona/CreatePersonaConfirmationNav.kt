package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.annotation.VisibleForTesting
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

@VisibleForTesting
internal const val ARG_PERSONA_ID = "arg_persona_id"
private const val ROUTE = "persona_completion_route/{$ARG_PERSONA_ID}"

fun NavController.createPersonaConfirmationScreen(personaId: String) {
    navigate("persona_completion_route/$personaId")
}

fun NavGraphBuilder.createPersonaConfirmationScreen(
    finishPersonaCreation: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
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
