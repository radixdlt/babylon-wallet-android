package com.babylon.wallet.android.presentation.createaccount.confirmation

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
internal const val ARG_ACCOUNT_ID = "arg_account_id"

@VisibleForTesting
internal const val ARG_REQUEST_SOURCE = "arg_request_source"

internal class CreateAccountConfirmationArgs(val accountId: String, val requestSource: CreateAccountRequestSource) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNT_ID]) as String,
        checkNotNull(savedStateHandle[ARG_REQUEST_SOURCE]) as CreateAccountRequestSource
    )
}

fun NavController.createAccountConfirmationScreen(accountId: String, requestSource: CreateAccountRequestSource) {
    navigate("account_completion_route/$requestSource/$accountId")
}

enum class CreateAccountRequestSource {
    AccountsList, ChooseAccount, FirstTime, Gateways
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createAccountConfirmationScreen(
    onNavigateToWallet: () -> Unit,
    onFinishAccountCreation: () -> Unit,
) {
    composable(
        route = "account_completion_route/{$ARG_REQUEST_SOURCE}/{$ARG_ACCOUNT_ID}",
        arguments = listOf(
            navArgument(ARG_REQUEST_SOURCE) {
                type = NavType.EnumType(
                    CreateAccountRequestSource::class.java
                )
            },
            navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
        )
    ) {
        CreateAccountConfirmationScreen(
            viewModel = hiltViewModel(),
            navigateToWallet = onNavigateToWallet,
            finishAccountCreation = onFinishAccountCreation
        )
    }
}
