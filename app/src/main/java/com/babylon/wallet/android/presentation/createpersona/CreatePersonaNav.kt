package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

const val ROUTE_CREATE_PERSONA = "create_persona_route"

fun NavController.createPersonaScreen() {
    navigate(ROUTE_CREATE_PERSONA)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createPersonaScreen(
    onBackClick: () -> Unit,
    onContinueClick: (personaId: String) -> Unit
) {
    composable(
        route = ROUTE_CREATE_PERSONA,
        arguments = listOf(),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        CreatePersonaScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}
