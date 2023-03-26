package com.babylon.wallet.android.presentation.dapp.accountonetime

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
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

internal class OneTimeChooseAccountsArgs(
    val dappId: String,
    val requestId: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_DAPP_ID]) as String,
        checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String
    )
}

fun NavController.chooseAccountsOneTime(dappId: String, requestId: String) {
    navigate("choose_accounts_route/$dappId/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.chooseAccountsOneTime(
    exitRequestFlow: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit
) {
    composable(
        route = "choose_accounts_route/{$ARG_DAPP_ID}/{$ARG_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_DAPP_ID) { type = NavType.StringType },
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType },
        )
    ) {
        OneTimeChooseAccountsScreen(
            viewModel = hiltViewModel(),
            exitRequestFlow = exitRequestFlow,
            dismissErrorDialog = dismissErrorDialog,
            onAccountCreationClick = onAccountCreationClick
        )
    }
}
