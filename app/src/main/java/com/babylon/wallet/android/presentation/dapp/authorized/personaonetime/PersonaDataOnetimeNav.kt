package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.dapp.model.PersonaDataField
import com.babylon.wallet.android.data.dapp.model.decodePersonaDataFields
import com.babylon.wallet.android.presentation.dapp.authorized.InitialAuthorizedLoginRoute
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeArgs(val requiredFields: Array<PersonaDataField>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataFields().toTypedArray()
    )
}

const val ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED =
    "route_persona_data_onetime_authorized/{$ARG_REQUIRED_FIELDS}}"

fun NavController.personaDataOnetimeAuthorized(requiredFieldsEncoded: String) {
    navigate("route_persona_data_onetime_authorized/$requiredFieldsEncoded")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDataOnetimeAuthorized(
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    sharedViewModel: DAppAuthorizedLoginViewModel,
    initialAuthorizedLoginRoute: InitialAuthorizedLoginRoute.OngoingPersonaData?,
    onBackClick: () -> Unit,
    onLoginFlowComplete: (String) -> Unit,
    onCreatePersona: () -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.SerializableArrayType(PersonaDataField::class.java)
                initialAuthorizedLoginRoute?.let {
                    defaultValue = initialAuthorizedLoginRoute.requestedFields.toTypedArray()
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
