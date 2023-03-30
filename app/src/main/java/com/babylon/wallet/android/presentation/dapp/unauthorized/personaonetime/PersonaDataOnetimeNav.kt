package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.unauthorized.InitialUnauthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.model.decodePersonaDataKinds
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.Network

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeUnauthorizedArgs(val requiredFields: Array<Network.Persona.Field.Kind>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataKinds().toTypedArray()
    )
}

const val ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED =
    "route_persona_data_onetime_unauthorized/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOnetimeUnauthorized(requiredFieldsEncoded: String) {
    navigate("route_persona_data_onetime_unauthorized/$requiredFieldsEncoded")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDataOnetimeUnauthorized(
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    sharedViewModel: DAppUnauthorizedLoginViewModel,
    initialDappLoginRoute: InitialUnauthorizedLoginRoute.OnetimePersonaData?,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (requestId: String, dAppName: String) -> Unit,
    onCreatePersona: (Boolean) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.StringType
                initialDappLoginRoute?.let {
                    defaultValue = initialDappLoginRoute.requestedFieldsEncoded
                }
            }
        )
    ) { entry ->
        PersonaDataOnetimeScreen(
            sharedViewModel = sharedViewModel,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete,
            onCreatePersona = onCreatePersona
        )
    }
}
