package com.babylon.wallet.android.presentation.dapp.unauthorized.login

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

const val ROUTE_DAPP_LOGIN_UNAUTHORIZED = "dapp_login_unauthorized/{$ARG_REQUEST_ID}"

internal class DAppUnauthorizedLoginArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.dAppLoginUnauthorized(requestId: String) {
    navigate("dapp_login_unauthorized/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
fun NavGraphBuilder.dAppLoginUnauthorized(
    navController: NavController,
    onBackClick: () -> Unit,
    showSuccessDialog: (String) -> Unit
) {
    composable(
        route = ROUTE_DAPP_LOGIN_UNAUTHORIZED,
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED)
        }
        val vm = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)
        DappUnauthorizedLoginScreen(
            viewModel = vm,
            onBackClick = onBackClick,
            showSuccessDialog = showSuccessDialog
        )
    }
}
