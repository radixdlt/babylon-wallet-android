package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.settings.personadetail.ROUTE_PERSONA_DETAIL
import com.babylon.wallet.android.presentation.settings.personas.PersonasScreen
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.factorsources.FactorSource

const val ROUTE_CREATE_PERSONA = "create_persona_route"
const val ROUTE_PERSONA_INFO = "persona_info_route"
const val ROUTE_PERSONAS = "personas_route"

fun NavController.createPersonaScreen() {
    navigate(ROUTE_CREATE_PERSONA)
}

fun NavController.personaScreen() {
    navigate(ROUTE_PERSONAS)
}

fun NavController.personaInfoScreen() {
    navigate(ROUTE_PERSONA_INFO)
}

@Suppress("SwallowedException")
fun NavController.popPersonaCreation() {
    val entryToPop = try {
        getBackStackEntry(ROUTE_PERSONA_INFO)
    } catch (e: java.lang.IllegalArgumentException) {
        getBackStackEntry(ROUTE_CREATE_PERSONA)
    }
    popBackStack(entryToPop.destination.id, true)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaInfoScreen(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    composable(
        route = ROUTE_PERSONA_INFO,
        arguments = listOf(),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        CreatePersonaInfoScreen(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createPersonaScreen(
    onBackClick: () -> Unit,
    onContinueClick: (personaId: String) -> Unit
) {
    markAsHighPriority(ROUTE_CREATE_PERSONA)
    composable(
        route = ROUTE_CREATE_PERSONA,
        arguments = listOf(),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        CreatePersonaScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personasScreen(
    onBackClick: () -> Unit,
    createPersonaScreen: (Boolean) -> Unit,
    onPersonaClick: (String) -> Unit,
    onNavigateToMnemonicBackup: (FactorSource.ID) -> Unit
) {
    composable(
        route = ROUTE_PERSONAS,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_PERSONAS, ROUTE_PERSONA_DETAIL, ROUTE_CREATE_PERSONA, ROUTE_PERSONA_INFO -> null
                else -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            }
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        PersonasScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            createNewPersona = createPersonaScreen,
            onPersonaClick = onPersonaClick,
            onNavigateToMnemonicBackup = onNavigateToMnemonicBackup
        )
    }
}
