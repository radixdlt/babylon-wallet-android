package com.babylon.wallet.android.presentation.settings.personas.personaedit

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.navigation.RequiredPersonaFieldsParameterType
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

const val ROUTE_EDIT_PERSONA = "persona_edit/{$ARG_PERSONA_ADDRESS}?$ARG_REQUIRED_FIELDS={$ARG_REQUIRED_FIELDS}"

internal class PersonaEditScreenArgs(
    val personaAddress: String,
    val requiredPersonaFields: RequiredPersonaFields? = null
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]) as String,
        savedStateHandle.get(ARG_REQUIRED_FIELDS) as? RequiredPersonaFields
    )
}

fun NavController.personaEditScreen(personaAddress: String, requiredPersonaFields: RequiredPersonaFields? = null) {
    var route = "persona_edit/$personaAddress"
    requiredPersonaFields?.let {
        val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(requiredPersonaFields))
        route += "?$ARG_REQUIRED_FIELDS=$argument"
    }
    navigate(route)
}

fun NavGraphBuilder.personaEditScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_EDIT_PERSONA,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            when (targetState.destination.route) {
                ROUTE_EDIT_PERSONA -> null
                else -> slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_PERSONA_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_REQUIRED_FIELDS) {
                type = RequiredPersonaFieldsParameterType
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
