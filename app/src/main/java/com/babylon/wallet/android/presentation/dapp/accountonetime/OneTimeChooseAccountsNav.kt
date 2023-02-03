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
internal const val ARG_REQUEST_ID = "request_id"

internal class UnauthorizedChooseAccountsArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.chooseAccountsOneTime(requestId: String) {
    navigate("choose_accounts_route/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.chooseAccountsOneTime(
    exitRequestFlow: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit
) {
    composable(
        route = "choose_accounts_route/{$ARG_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType },
        )
    ) {
        UnauthorizedChooseAccountsScreen(
            viewModel = hiltViewModel(),
            exitRequestFlow = exitRequestFlow,
            dismissErrorDialog = dismissErrorDialog,
            onAccountCreationClick = onAccountCreationClick
        )
    }
}
