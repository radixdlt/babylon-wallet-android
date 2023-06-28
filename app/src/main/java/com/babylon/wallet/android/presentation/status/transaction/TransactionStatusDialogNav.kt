package com.babylon.wallet.android.presentation.status.transaction

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.transaction.ROUTE_TRANSACTION_APPROVAL
import com.babylon.wallet.android.presentation.transfer.ROUTE_TRANSFER
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.routeExist
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleForTesting
private const val ARG_STATUS = "arg_status"

@VisibleForTesting
private const val VALUE_STATUS_SUCCESS = "success"

@VisibleForTesting
private const val VALUE_STATUS_FAIL = "fail"

@VisibleForTesting
private const val VALUE_STATUS_IN_PROGRESS = "in_progress"

@VisibleForTesting
private const val ARG_REQUEST_ID = "arg_request_id"

@VisibleForTesting
private const val ARG_TX_ID = "arg_tx_id"

@VisibleForTesting
private const val ARG_IS_INTERNAL = "arg_is_internal"

@VisibleForTesting
private const val ARG_ERROR = "arg_error"

@VisibleForTesting
private const val ROUTE = "transaction_status_dialog"

internal class TransactionStatusDialogArgs(
    val event: AppEvent.Status.Transaction
) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.toStatus())
}

fun NavController.transactionStatusDialog(transactionEvent: AppEvent.Status.Transaction) {
    // Do not create another entry when this dialog exists
    // New requests will be handled from the view model itself
    if (currentBackStackEntry?.destination?.route?.startsWith(ROUTE) == true) return

    val errorSerialized = if (transactionEvent is AppEvent.Status.Transaction.Fail) {
        Json.encodeToString(transactionEvent.errorMessage)
    } else {
        null
    }

    val requestId = transactionEvent.requestId.ifBlank { error("Transaction id cannot be empty") }
    navigate(
        route = "$ROUTE/${transactionEvent.toType()}/$requestId" +
            "?txId=${transactionEvent.transactionId}" +
            "&isInternal=${transactionEvent.isInternal}" +
            "&error=$errorSerialized"
    ) {
        val popUpToRoute = if (this@transactionStatusDialog.routeExist(ROUTE_TRANSFER)) {
            ROUTE_TRANSFER
        } else {
            ROUTE_TRANSACTION_APPROVAL
        }

        popUpTo(route = popUpToRoute) {
            inclusive = true
        }
    }
}

fun NavGraphBuilder.transactionStatusDialog(
    onClose: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_STATUS}/{$ARG_REQUEST_ID}?txId={$ARG_TX_ID}&isInternal={$ARG_IS_INTERNAL}&error={$ARG_ERROR}",
        arguments = listOf(
            navArgument(ARG_STATUS) {
                type = NavType.StringType
            },
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_TX_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(ARG_IS_INTERNAL) {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument(ARG_ERROR) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TransactionStatusDialog(
            viewModel = hiltViewModel(),
            onClose = onClose
        )
    }
}

private fun AppEvent.Status.Transaction.toType() = when (this) {
    is AppEvent.Status.Transaction.Fail -> VALUE_STATUS_FAIL
    is AppEvent.Status.Transaction.InProgress -> VALUE_STATUS_IN_PROGRESS
    is AppEvent.Status.Transaction.Success -> VALUE_STATUS_SUCCESS
}

private fun SavedStateHandle.toStatus(): AppEvent.Status.Transaction {
    return when (checkNotNull(get<String>(ARG_STATUS))) {
        VALUE_STATUS_FAIL -> AppEvent.Status.Transaction.Fail(
            requestId = checkNotNull(get<String>(ARG_REQUEST_ID)),
            transactionId = checkNotNull(get<String>(ARG_TX_ID)),
            isInternal = checkNotNull(get<Boolean>(ARG_IS_INTERNAL)),
            errorMessage = get<String>(ARG_ERROR)?.let { Json.decodeFromString(it) },
        )
        VALUE_STATUS_SUCCESS -> AppEvent.Status.Transaction.Success(
            requestId = checkNotNull(get<String>(ARG_REQUEST_ID)),
            transactionId = checkNotNull(get<String>(ARG_TX_ID)),
            isInternal = checkNotNull(get<Boolean>(ARG_IS_INTERNAL)),
        )
        VALUE_STATUS_IN_PROGRESS -> AppEvent.Status.Transaction.InProgress(
            requestId = checkNotNull(get<String>(ARG_REQUEST_ID)),
            transactionId = checkNotNull(get<String>(ARG_TX_ID)),
            isInternal = checkNotNull(get<Boolean>(ARG_IS_INTERNAL)),
        )
        else -> error("Status not received")
    }
}
