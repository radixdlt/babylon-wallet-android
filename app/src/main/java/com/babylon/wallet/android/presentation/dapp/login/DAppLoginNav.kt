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
internal const val ARG_DAPP_ID = "arg_dapp_id"
internal const val ARG_REQUEST_ID = "arg_request_id"

const val ROUTE_DAPP_LOGIN = "dapp_login/{$ARG_DAPP_ID}/{$ARG_REQUEST_ID}"

internal class DAppLoginArgs(
    val dappId: String,
    val requestId: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_DAPP_ID]) as String,
        checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String
    )
}

fun NavController.dAppLogin(dappId: String, requestId: String) {
    navigate("dapp_login/$dappId/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dAppLogin(
    navController: NavController,
    onBackClick: () -> Unit,
    showSuccessDialog: (String) -> Unit
) {
    composable(
        route = ROUTE_DAPP_LOGIN,
        arguments = listOf(
            navArgument(ARG_DAPP_ID) { type = NavType.StringType },
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN)
        }
        val vm = hiltViewModel<DAppLoginViewModel>(parentEntry)
        DappLoginScreen(
            viewModel = vm,
            onBackClick = onBackClick,
            showSuccessDialog = showSuccessDialog
        )
    }
}
