package com.babylon.wallet.android.presentation.settings.personas.createpersona

import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.settings.personas.PersonasScreen
import com.radixdlt.sargon.IdentityAddress
import rdx.works.core.flatMapError

const val ARG_REQUEST_SOURCE = "arg_request_source"
const val ROUTE_CREATE_PERSONA = "create_persona_route/{$ARG_REQUEST_SOURCE}"
const val ROUTE_PERSONA_INFO = "persona_info_route/{$ARG_REQUEST_SOURCE}"
const val ROUTE_PERSONAS = "personas_route"

enum class CreatePersonaRequestSource {
    Settings, DappRequest
}

internal fun NavBackStackEntry.getCreatePersonaRequestSource(): CreatePersonaRequestSource {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        checkNotNull(arguments?.getSerializable(ARG_REQUEST_SOURCE, CreatePersonaRequestSource::class.java))
    } else {
        arguments?.getSerializable(ARG_REQUEST_SOURCE) as CreatePersonaRequestSource
    }
}

fun NavController.createPersonaScreen(requestSource: CreatePersonaRequestSource) {
    navigate("create_persona_route/$requestSource")
}

fun NavController.personasScreen() {
    navigate(ROUTE_PERSONAS) {
        launchSingleTop = true
    }
}

fun NavController.personaInfoScreen(requestSource: CreatePersonaRequestSource) {
    navigate("persona_info_route/$requestSource")
}

fun NavController.popPersonaCreation() {
    val entryToPop = runCatching { getBackStackEntry(ROUTE_PERSONA_INFO) }.flatMapError {
        runCatching { getBackStackEntry(ROUTE_CREATE_PERSONA) }
    }.getOrNull()
    if (entryToPop == null) {
        popBackStack()
    } else {
        popBackStack(entryToPop.destination.id, true)
    }
}

fun NavGraphBuilder.personaInfoScreen(
    onInfoClick: (GlossaryItem) -> Unit,
    onContinueClick: (CreatePersonaRequestSource) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_PERSONA_INFO,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(CreatePersonaRequestSource::class.java)
            }
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
        val requestSource = it.getCreatePersonaRequestSource()
        CreatePersonaInfoScreen(
            onInfoClick = onInfoClick,
            onContinueClick = {
                onContinueClick(requestSource)
            },
            onBackClick = onBackClick
        )
    }
}

fun NavGraphBuilder.createPersonaScreen(
    onContinueClick: (CreatePersonaRequestSource) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    markAsHighPriority(ROUTE_CREATE_PERSONA)
    composable(
        route = ROUTE_CREATE_PERSONA,
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(CreatePersonaRequestSource::class.java)
            }
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
        val requestSource = it.getCreatePersonaRequestSource()
        CreatePersonaScreen(
            viewModel = hiltViewModel(),
            onContinueClick = {
                onContinueClick(requestSource)
            },
            onBackClick = onBackClick,
            onInfoClick = onInfoClick
        )
    }
}

fun NavGraphBuilder.personasScreen(
    onBackClick: () -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    onPersonaClick: (IdentityAddress) -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    composable(
        route = ROUTE_PERSONAS,
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
        PersonasScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            createNewPersona = onCreatePersona,
            onPersonaClick = onPersonaClick,
            onNavigateToSecurityCenter = onNavigateToSecurityCenter,
            onInfoClick = onInfoClick
        )
    }
}
