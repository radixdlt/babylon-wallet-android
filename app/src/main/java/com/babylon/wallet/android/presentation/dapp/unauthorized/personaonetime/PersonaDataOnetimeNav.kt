package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

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
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.navigation.PersonaRequestItemParameterType
import com.google.accompanist.navigation.animation.composable
import kotlinx.serialization.encodeToString

@VisibleForTesting
internal const val ARG_REQUIRED_FIELDS = "required_fields"

internal class PersonaDataOnetimeUnauthorizedArgs(val request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        (checkNotNull(savedStateHandle[ARG_REQUIRED_FIELDS]) as MessageFromDataChannel.IncomingRequest.PersonaRequestItem)
    )
}

const val ROUTE_PERSONA_DATA_ONETIME_UNAUTHORIZED =
    "route_persona_data_onetime_unauthorized/{$ARG_REQUIRED_FIELDS}"

fun NavController.personaDataOnetimeUnauthorized(request: MessageFromDataChannel.IncomingRequest.PersonaRequestItem) {
    val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(request))
    navigate("route_persona_data_onetime_unauthorized/$argument")
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
                type = PersonaRequestItemParameterType
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
