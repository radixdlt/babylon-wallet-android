package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.model.decodePersonaDataKinds
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.Network

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeArgs(val requiredFields: Array<Network.Persona.Field.Kind>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataKinds().toTypedArray()
    )
}

const val ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED =
    "route_persona_data_onetime_authorized/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOnetimeAuthorized(requiredFieldsEncoded: String) {
    navigate("route_persona_data_onetime_authorized/$requiredFieldsEncoded")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDataOnetimeAuthorized(
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute.OneTimePersonaData?,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (DAppAuthorizedLoginEvent.LoginFlowCompleted) -> Unit,
    onCreatePersona: (Boolean) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.StringType
                initialAuthorizedLoginRoute?.let {
                    defaultValue = initialAuthorizedLoginRoute.requestedFieldsEncoded
                }
            }
        )
    ) {
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
