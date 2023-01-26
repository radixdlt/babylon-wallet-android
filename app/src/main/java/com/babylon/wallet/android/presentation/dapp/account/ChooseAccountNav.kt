package com.babylon.wallet.android.presentation.dapp.account

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_ACCOUNTS_REQUEST_ID = "arg_accounts_request_id"

internal class ChooseAccountsScreenArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNTS_REQUEST_ID]) as String
    )
}

fun NavController.chooseAccountsScreen(requestId: String) {
    navigate("choose_accounts_route/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.chooseAccountsScreen(
    onBackClick: () -> Unit,
    onAccountCreationClick: () -> Unit
) {
    composable(
        route = "choose_accounts_route/{$ARG_ACCOUNTS_REQUEST_ID}"
    ) {
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            exitRequestFlow = onBackClick,
            dismissErrorDialog = onBackClick,
            onAccountCreationClick = onAccountCreationClick
        )
    }
}
