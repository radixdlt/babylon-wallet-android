package com.babylon.wallet.android.presentation.transaction

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
internal const val ARG_TRANSACTION_DAPP_ID = "arg_transaction_dapp_id"
internal const val ARG_TRANSACTION_REQUEST_ID = "arg_transaction_request_id"

internal class TransactionApprovalArgs(
    val dappId: String,
    val requestId: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_TRANSACTION_DAPP_ID]) as String,
        checkNotNull(savedStateHandle[ARG_TRANSACTION_REQUEST_ID]) as String
    )
}

fun NavController.transactionApproval(
    dappId: String,
    requestId: String
) {
    navigate("transaction_approval_route/$dappId/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.transactionApprovalScreen(onBackClick: () -> Unit) {
    composable(
        route = "transaction_approval_route/{$ARG_TRANSACTION_DAPP_ID}/{$ARG_TRANSACTION_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_TRANSACTION_DAPP_ID) { type = NavType.StringType },
            navArgument(ARG_TRANSACTION_REQUEST_ID) { type = NavType.StringType }
        )
    ) {
        TransactionApprovalScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
