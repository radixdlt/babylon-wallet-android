package com.babylon.wallet.android.presentation.transactionstatus

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

@VisibleForTesting
private const val ARG_REQUEST_ID = "arg_request_id"

internal class TransactionStatusDialogArgs(
    val requestId: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<String>(ARG_REQUEST_ID))
    )
}

fun NavController.transactionStatusDialog(requestId: String) {
    navigate("transaction_status_dialog/$requestId")
}

fun NavGraphBuilder.transactionStatusDialog(
    onBackPress: () -> Unit
) {
    dialog(
        route = "transaction_status_dialog/{$ARG_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TransactionStatusDialog(
            viewModel = hiltViewModel(),
            onBackPress = onBackPress
        )
    }
}
