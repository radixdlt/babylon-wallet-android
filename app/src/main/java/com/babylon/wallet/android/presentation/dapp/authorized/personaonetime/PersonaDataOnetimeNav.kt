package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.navigation.PersonaRequestItemParameterType
import com.google.accompanist.navigation.animation.composable
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeArgs(val request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as MessageFromDataChannel.IncomingRequest.PersonaRequestItem)
    )
}

const val ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED =
    "route_persona_data_onetime_authorized/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOnetimeAuthorized(request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
    val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(request))
    navigate("route_persona_data_onetime_authorized/$argument")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.personaDataOnetimeAuthorized(
    onEdit: (PersonaDataOnetimeEvent.OnEditPersona) -> Unit,
    onBackClick: () -> Unit,
    navController: NavController,
    onLoginFlowComplete: () -> Unit,
    onCreatePersona: (Boolean) -> Unit
) {
    composable(
        route = ROUTE_PERSONA_DATA_ONETIME_AUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUIRED_FIELDS) {
                type = PersonaRequestItemParameterType
            }
        )
    ) {
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        PersonaDataOnetimeScreen(
            sharedViewModel = sharedVM,
            onEdit = onEdit,
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
            onLoginFlowComplete = onLoginFlowComplete,
            onCreatePersona = onCreatePersona
        )
    }
}