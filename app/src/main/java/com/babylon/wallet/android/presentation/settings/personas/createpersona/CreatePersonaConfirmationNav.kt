package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_PERSONA_ID = "arg_persona_id"
private const val ROUTE = "persona_completion_route/{$ARG_PERSONA_ID}"

fun NavController.createPersonaConfirmationScreen(personaId: String) {
    navigate("persona_completion_route/$personaId")
}

@OptIn(ExperimentalAnimationApi::class)
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
