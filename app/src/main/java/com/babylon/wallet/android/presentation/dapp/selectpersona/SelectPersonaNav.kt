package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.InitialDappLoginRoute
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_REQUEST_ID = "request_id"

const val ROUTE_SELECT_PERSONA = "select_persona/{$ARG_REQUEST_ID}"

internal class SelectPersonaArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
fun NavGraphBuilder.selectPersona(
    onBackClick: () -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit,
    createNewPersona: () -> Unit,
    initialDappLoginRoute: InitialDappLoginRoute.SelectPersona?,
    sharedViewModel: DAppLoginViewModel,
    onDisplayPermission: (DAppLoginEvent.DisplayPermission) -> Unit
) {
    composable(
        route = ROUTE_SELECT_PERSONA,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
                nullable = false
                initialDappLoginRoute?.let {
                    defaultValue = initialDappLoginRoute.reqId
                }
            }
        )
    ) {
        SelectPersonaScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedViewModel,
            onBackClick = onBackClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            createNewPersona = createNewPersona,
            onDisplayPermission = onDisplayPermission
        )
    }
}
