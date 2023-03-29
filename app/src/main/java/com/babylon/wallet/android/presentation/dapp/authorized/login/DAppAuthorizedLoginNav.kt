package com.babylon.wallet.android.presentation.dapp.authorized.login

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

const val ROUTE_DAPP_LOGIN_AUTHORIZED = "dapp_login_authorized/{$ARG_REQUEST_ID}"

internal class DAppAuthorizedLoginArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.dAppLoginAuthorized(requestId: String) {
    navigate("dapp_login_authorized/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
fun NavGraphBuilder.dAppLoginAuthorized(
    navController: NavController,
    onBackClick: () -> Unit,
    showSuccessDialog: (String) -> Unit
) {
    composable(
        route = ROUTE_DAPP_LOGIN_AUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED)
        }
        val vm = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        DappAuthorizedLoginScreen(
            viewModel = vm,
            onBackClick = onBackClick,
            showSuccessDialog = showSuccessDialog
        )
    }
}
