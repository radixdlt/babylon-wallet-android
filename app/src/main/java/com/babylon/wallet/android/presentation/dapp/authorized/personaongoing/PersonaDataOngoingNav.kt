package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.google.accompanist.navigation.animation.composable
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

@VisibleForTesting
internal const val ARG_PERSONA_ID = "persona_id"

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOngoingPermissionArgs(val personaId: String, val requiredFields: Array<PersonaDataEntryID>) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_PERSONA_ID]) as String,
        // TODO persona data
//        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as String).decodePersonaDataKinds()
//            .toTypedArray()
        emptyArray()
    )
}

const val ROUTE_PERSONA_DATA_ONGOING =
    "route_persona_data_ongoing/{$ARG_PERSONA_ID}/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOngoing(personaAddress: String, fieldsEncoded: String) {
    navigate("route_persona_data_ongoing/$personaAddress/$fieldsEncoded")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
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
                type = NavType.StringType
            }
        )
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
