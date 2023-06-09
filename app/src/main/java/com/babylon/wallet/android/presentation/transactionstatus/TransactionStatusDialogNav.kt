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
import com.babylon.wallet.android.utils.AppEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleForTesting
private const val ARG_REQUEST_EVENT = "arg_request_event"

internal class TransactionStatusDialogArgs(
    val event: AppEvent.TransactionEvent
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<String>(ARG_REQUEST_EVENT)?.let { Json.decodeFromString<AppEvent.TransactionEvent>(it) })
    )
}

fun NavController.transactionStatusDialogShown(): Boolean {
    return currentBackStackEntry?.destination?.route?.startsWith("transaction_status_dialog") == true
}

fun NavController.transactionStatusDialog(transactionEvent: AppEvent.TransactionEvent) {
    val serialized = Json.encodeToString(transactionEvent)
    navigate("transaction_status_dialog/$serialized")
}

fun NavGraphBuilder.transactionStatusDialog(
    onClose: () -> Unit
) {
    dialog(
        route = "transaction_status_dialog/{$ARG_REQUEST_EVENT}",
        arguments = listOf(
            navArgument(ARG_REQUEST_EVENT) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TransactionStatusDialog(
            viewModel = hiltViewModel(),
            onClose = onClose
        )
    }
}
