package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

const val ROUTE_EDIT_PERSONA = "persona_edit/{$ARG_PERSONA_ADDRESS}"

internal class PersonaEditScreenArgs(val personaAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]) as String
    )
}

fun NavController.personaEditScreen(personaAddress: String) {
    navigate("persona_edit/$personaAddress")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaEditScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_EDIT_PERSONA,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA -> null
                else -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_PERSONA_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        PersonaEditScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
