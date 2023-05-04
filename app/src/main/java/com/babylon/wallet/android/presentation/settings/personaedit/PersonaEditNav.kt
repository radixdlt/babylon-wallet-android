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
import com.babylon.wallet.android.presentation.model.decodePersonaDataKinds
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.Network

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

const val ROUTE_EDIT_PERSONA = "persona_edit/{$ARG_PERSONA_ADDRESS}?$ARG_REQUIRED_FIELDS={$ARG_REQUIRED_FIELDS}"

internal class PersonaEditScreenArgs(val personaAddress: String, val requiredFields: Array<Network.Persona.Field.ID> = emptyArray()) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]) as String,
        savedStateHandle.get<String>(ARG_REQUIRED_FIELDS)?.decodePersonaDataKinds().orEmpty().toTypedArray()
    )
}

fun NavController.personaEditScreen(personaAddress: String, fieldsEncoded: String? = null) {
    var route = "persona_edit/$personaAddress"
    fieldsEncoded?.let {
        route += "?$ARG_REQUIRED_FIELDS=$it"
    }
    navigate(route)
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
            },
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        PersonaEditScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
