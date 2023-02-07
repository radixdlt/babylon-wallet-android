package com.babylon.wallet.android.presentation.dapp.login

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_REQUEST_ID = "request_id"

const val ROUTE_DAPP_LOGIN = "dapp_login/{$ARG_REQUEST_ID}"

internal class DAppLoginArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.dAppLogin(requestId: String) {
    navigate("dapp_login/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
fun NavGraphBuilder.dAppLogin(
    onBackClick: () -> Unit,
    navController: NavController,
    onHandleOngoingAccounts: (DAppLoginEvent.HandleOngoingAccounts) -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit,
    createNewPersona: () -> Unit
) {
    composable(
        route = ROUTE_DAPP_LOGIN,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN)
        }
        val vm = hiltViewModel<DAppLoginViewModel>(parentEntry)
        DAppLoginScreen(
            viewModel = vm,
            onBackClick = onBackClick,
            onHandleOngoingAccounts = onHandleOngoingAccounts,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            createNewPersona = createNewPersona
        )
    }
}
