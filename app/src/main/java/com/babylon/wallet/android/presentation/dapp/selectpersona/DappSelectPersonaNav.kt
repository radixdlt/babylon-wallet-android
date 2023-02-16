package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.InitialRoute
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_REQUEST_ID = "request_id"

const val ROUTE_MAIN_DAPP_LOGIN = "select_persona/{$ARG_REQUEST_ID}"

fun NavController.selectPersona(requestId: String) {
    navigate("main_login/$requestId")
}

internal class DappSelectPersonaArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
fun NavGraphBuilder.selectPersona(
    onBackClick: () -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit,
    createNewPersona: () -> Unit,
    initialRoute: InitialRoute.SelectPersona?,
    sharedViewModel: DAppLoginViewModel
) {
    composable(
        route = ROUTE_MAIN_DAPP_LOGIN,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
                nullable = false
                initialRoute?.let {
                    defaultValue = initialRoute.reqId
                }
            }
        )
    ) {
        DappSelectPersonaScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedViewModel,
            onBackClick = onBackClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            createNewPersona = createNewPersona,
        )
    }
}
