package com.babylon.wallet.android.presentation.settings.personas.personaedit

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.navigation.RequiredPersonaFieldsParameterType
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_PERSONA_ADDRESS = "persona_address"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

const val ROUTE_EDIT_PERSONA = "persona_edit/{$ARG_PERSONA_ADDRESS}?$ARG_REQUIRED_FIELDS={$ARG_REQUIRED_FIELDS}"

internal class PersonaEditScreenArgs(
    val personaAddress: IdentityAddress,
    val requiredPersonaFields: RequiredPersonaFields? = null
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        personaAddress = IdentityAddress.init(requireNotNull(savedStateHandle[ARG_PERSONA_ADDRESS])),
        savedStateHandle.get(ARG_REQUIRED_FIELDS) as? RequiredPersonaFields
    )
}

fun NavController.personaEditScreen(personaAddress: IdentityAddress, requiredPersonaFields: RequiredPersonaFields? = null) {
    var route = "persona_edit/${personaAddress.string}"
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
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
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
