package com.babylon.wallet.android.presentation.ui.composables.status.transaction

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.htmlEncode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.utils.AppEvent
import timber.log.Timber

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
private const val ARG_ERROR_MESSAGE = "arg_error_message"

@VisibleForTesting
private const val ROUTE = "transaction_status_dialog"

internal class TransactionStatusDialogArgs(
    val event: AppEvent.Status.Transaction
) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.toStatus())
}

fun NavController.transactionStatusDialogShown(): Boolean {
    return currentBackStackEntry?.destination?.route?.startsWith(ROUTE) == true
}

fun NavController.transactionStatusDialog(transactionEvent: AppEvent.Status.Transaction) {
    val errorMessageRes = if (transactionEvent is AppEvent.Status.Transaction.Fail) {
        transactionEvent.errorMessageRes.toString()
    } else {
        null
    }

    val requestId = transactionEvent.requestId.ifBlank { error("Transaction id cannot be empty") }
    navigate(
        route = "$ROUTE/${transactionEvent.toType()}/$requestId" +
                "?txId=${transactionEvent.transactionId}" +
                "&isInternal=${transactionEvent.isInternal}" +
                "&error=${errorMessageRes}"
    )
}

fun NavGraphBuilder.transactionStatusDialog(
    onClose: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_STATUS}/{$ARG_REQUEST_ID}?txId={$ARG_TX_ID}&isInternal={$ARG_IS_INTERNAL}&error={$ARG_ERROR_MESSAGE}",
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
            navArgument(ARG_ERROR_MESSAGE) {
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
            errorMessageRes = get<String>(ARG_ERROR_MESSAGE)?.toInt(),
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
