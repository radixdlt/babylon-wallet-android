package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeUnauthorizedArgs(val requiredFields: Array<PersonaDataEntryID>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        // TODO persona edit
//        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataKinds().toTypedArray()
        emptyArray()
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
    onBackClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onCreatePersona: (Boolean) -> Unit,
    navController: NavController,
    onLoginFlowCancelled: () -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)
        PersonaDataOnetimeScreen(
            sharedViewModel = sharedVM,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete,
            onCreatePersona = onCreatePersona,
            onLoginFlowCancelled = onLoginFlowCancelled
        )
    }
}
