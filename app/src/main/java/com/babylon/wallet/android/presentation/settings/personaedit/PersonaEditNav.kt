package com.babylon.wallet.android.presentation.settings.personaedit

import android.net.Uri
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
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.RequiredFields
import com.babylon.wallet.android.presentation.navigation.RequiredFieldsParameterType
import com.google.accompanist.navigation.animation.composable
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

const val ROUTE_EDIT_PERSONA = "persona_edit/{$ARG_PERSONA_ADDRESS}?$ARG_REQUIRED_FIELDS={$ARG_REQUIRED_FIELDS}"

internal class PersonaEditScreenArgs(
    val personaAddress: String,
    val requiredFields: RequiredFields? = null
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ADDRESS]) as String,
        savedStateHandle.get(ARG_REQUIRED_FIELDS) as? RequiredFields
    )
}

fun NavController.personaEditScreen(personaAddress: String, requiredFields: RequiredFields? = null) {
    var route = "persona_edit/$personaAddress"
    requiredFields?.let {
        val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(requiredFields))
        route += "?$ARG_REQUIRED_FIELDS=$argument"
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
                type = RequiredFieldsParameterType
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
