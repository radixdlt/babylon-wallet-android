package com.babylon.wallet.android.presentation.dapp.unauthorized.login

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.RequiredPersonaFields

@VisibleForTesting
internal const val ARG_REQUEST_ID = "request_id"

const val ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH = "dapp_login_unauthorized/{$ARG_REQUEST_ID}"
const val ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN = "dapp_login_unauthorized_screen/{$ARG_REQUEST_ID}"

internal class DAppUnauthorizedLoginArgs(val interactionId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.dAppLoginUnauthorized(requestId: String) {
    navigate("dapp_login_unauthorized/$requestId")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.dAppLoginUnauthorized(
    navController: NavController,
    navigateToChooseAccount: (Int, Boolean) -> Unit,
    navigateToOneTimePersonaData: (RequiredPersonaFields) -> Unit,
    onLoginFlowComplete: () -> Unit
) {
    composable(
        route = ROUTE_DAPP_LOGIN_UNAUTHORIZED_SCREEN,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH)
        }
        val vm = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)
        DappUnauthorizedLoginScreen(
            viewModel = vm,
            navigateToOneTimePersonaData = navigateToOneTimePersonaData,
            navigateToChooseAccount = navigateToChooseAccount,
            onLoginFlowComplete = onLoginFlowComplete
        )
    }
}
