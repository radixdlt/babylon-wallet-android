package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.model.decodePersonaDataKinds
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.Network

@VisibleForTesting
internal const val ARG_PERSONA_ID = "persona_id"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOngoingPermissionArgs(val personaId: String, val requiredFields: Array<Network.Persona.Field.Kind>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ID]) as String,
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataKinds()
            .toTypedArray()
    )
}

const val ROUTE_PERSONA_DATA_ONGOING =
    "route_persona_data_ongoing/{$ARG_PERSONA_ID}/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOngoing(personaId: String, fieldsEncoded: String) {
    navigate("route_persona_data_ongoing/$personaId/$fieldsEncoded")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDataOngoing(
    onEdit: (PersonaDataOngoingEvent.OnEditPersona) -> Unit,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute.OngoingPersonaData?,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (String) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONGOING,
        arguments = listOf(
            navArgument(ARG_PERSONA_ID) {
                type = NavType.StringType
                initialAuthorizedLoginRoute?.let {
                    defaultValue = initialAuthorizedLoginRoute.personaAddress
                }
            },
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.StringType
                initialAuthorizedLoginRoute?.let {
                    defaultValue = initialAuthorizedLoginRoute.requestedFieldsEncoded
                }
            }
        )
    ) { entry ->
        PersonaDataOngoingScreen(
            sharedViewModel = sharedViewModel,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete
        )
    }
}
