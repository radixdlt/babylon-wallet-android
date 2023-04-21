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
internal const val ARG_TRANSACTION_REQUEST_ID = "arg_transaction_request_id"

internal class TransactionApprovalArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_TRANSACTION_REQUEST_ID]) as String
    )
}

fun NavController.transactionApproval(requestId: String) {
    navigate("transaction_approval_route/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.transactionApprovalScreen(
    onBackClick: () -> Unit,
    showSuccessDialog: (requestId: String) -> Unit,
    showErrorDialog: (requestId: String, errorTextRes: Int) -> Unit
) {
    composable(
        route = "transaction_approval_route/{$ARG_TRANSACTION_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_TRANSACTION_REQUEST_ID) { type = NavType.StringType }
        )
    ) {
        TransactionApprovalScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            showSuccessDialog = showSuccessDialog,
            showErrorDialog = showErrorDialog
        )
    }
}
