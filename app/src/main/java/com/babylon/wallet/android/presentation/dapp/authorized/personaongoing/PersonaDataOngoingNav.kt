package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.navigation.RequiredPersonaFieldsParameterType
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_PERSONA_ID = "persona_id"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

@VisibleForTesting
internal const val ARG_SHOW_BACK = "show_back"

internal class PersonaDataOngoingPermissionArgs(
    val personaId: IdentityAddress,
    val requiredPersonaFields: RequiredPersonaFields,
    val showBack: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        IdentityAddress.init(checkNotNull(savedStateHandle[ARG_PERSONA_ID])),
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as RequiredPersonaFields),
        checkNotNull(savedStateHandle[ARG_SHOW_BACK])
    )
}

const val ROUTE_PERSONA_DATA_ONGOING =
    "route_persona_data_ongoing/{$ARG_PERSONA_ID}/{$ARG_REQUIRED_FIELDS}/{$ARG_SHOW_BACK}"

fun NavController.personaDataOngoing(personaAddress: IdentityAddress, request: RequiredPersonaFields, showBack: Boolean) {
    val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(request))
    navigate("route_persona_data_ongoing/${personaAddress.string}/$argument/$showBack")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.personaDataOngoing(
    onEdit: (PersonaDataOngoingEvent.OnEditPersona) -> Unit,
    onBackClick: () -> Unit,
    navController: NavController,
    onLoginFlowComplete: () -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONGOING,
        arguments = listOf(
            navArgument(ARG_PERSONA_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_REQUIRED_FIELDS) {
                type = RequiredPersonaFieldsParameterType
            },
            navArgument(ARG_SHOW_BACK) {
                type = NavType.BoolType
            }
        ),
        enterTransition = {
            if (requiresHorizontalTransition(targetState.arguments)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            }
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (requiresHorizontalTransition(initialState.arguments)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        }
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        PersonaDataOngoingScreen(
            sharedViewModel = sharedVM,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete,
            onPersonaDataOnetime = onPersonaDataOnetime,
            onChooseAccounts = onChooseAccounts
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_SHOW_BACK)
}
