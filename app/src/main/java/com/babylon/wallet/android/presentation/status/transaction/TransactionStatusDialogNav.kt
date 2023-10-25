package com.babylon.wallet.android.presentation.status.transaction

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.domain.model.transaction.TransactionStatusFields
import com.babylon.wallet.android.domain.model.transaction.TransactionStatusParameterType
import com.babylon.wallet.android.presentation.transaction.ROUTE_TRANSACTION_REVIEW
import com.babylon.wallet.android.presentation.transfer.ROUTE_TRANSFER
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.routeExist
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleForTesting
private const val VALUE_STATUS_SUCCESS = "success"

@VisibleForTesting
private const val VALUE_STATUS_FAIL = "fail"

@VisibleForTesting
private const val VALUE_STATUS_IN_PROGRESS = "in_progress"

@VisibleForTesting
private const val ROUTE = "transaction_status_dialog"

@VisibleForTesting
private const val ARG_TRANSACTION_STATUS_FIELDS = "arg_transaction_status_fields"

internal class TransactionStatusDialogArgs(
    val event: AppEvent.Status.Transaction
) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.toStatus())
}

fun NavController.transactionStatusDialog(transactionEvent: AppEvent.Status.Transaction) {
    // Do not create another entry when this dialog exists
    // New requests will be handled from the view model itself
    if (currentBackStackEntry?.destination?.route?.startsWith(ROUTE) == true) return

    val argument = Uri.encode(Serializer.kotlinxSerializationJson.encodeToString(transactionEvent.toTransactionStatusFields()))
    navigate("transaction_status_dialog/$argument") {
        val popUpToRoute = if (this@transactionStatusDialog.routeExist(ROUTE_TRANSFER)) {
            ROUTE_TRANSFER
        } else {
            ROUTE_TRANSACTION_REVIEW
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
        route = "transaction_status_dialog/{$ARG_TRANSACTION_STATUS_FIELDS}",
        arguments = listOf(
            navArgument(ARG_TRANSACTION_STATUS_FIELDS) {
                type = TransactionStatusParameterType
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

private fun AppEvent.Status.Transaction.toStatus() = when (this) {
    is AppEvent.Status.Transaction.Fail -> VALUE_STATUS_FAIL
    is AppEvent.Status.Transaction.InProgress -> VALUE_STATUS_IN_PROGRESS
    is AppEvent.Status.Transaction.Success -> VALUE_STATUS_SUCCESS
}

fun AppEvent.Status.Transaction.toTransactionStatusFields(): TransactionStatusFields {
    val requestId = requestId.ifBlank { error("Transaction id cannot be empty") }

    val errorSerialized = if (this is AppEvent.Status.Transaction.Fail) {
        Json.encodeToString(errorMessage)
    } else {
        null
    }

    val txProcessingTime = if (this is AppEvent.Status.Transaction.Fail) {
        Json.encodeToString(txProcessingTime)
    } else {
        null
    }

    val walletErrorType = if (this is AppEvent.Status.Transaction.Fail) {
        Json.encodeToString(walletErrorType)
    } else {
        null
    }

    return TransactionStatusFields(
        status = toStatus(),
        requestId = requestId,
        transactionId = transactionId,
        isInternal = isInternal,
        error = errorSerialized,
        transactionProcessingTime = txProcessingTime,
        walletErrorType = walletErrorType,
        blockUntilComplete = blockUntilComplete
    )
}

private fun SavedStateHandle.toStatus(): AppEvent.Status.Transaction {
    val transactionStatusFields = checkNotNull(get<TransactionStatusFields>(ARG_TRANSACTION_STATUS_FIELDS))
    return when (checkNotNull(transactionStatusFields.status)) {
        VALUE_STATUS_FAIL -> AppEvent.Status.Transaction.Fail(
            requestId = checkNotNull(transactionStatusFields.requestId),
            transactionId = checkNotNull(transactionStatusFields.transactionId),
            isInternal = checkNotNull(transactionStatusFields.isInternal),
            errorMessage = transactionStatusFields.error?.let { Json.decodeFromString(it) },
            blockUntilComplete = checkNotNull(transactionStatusFields.blockUntilComplete),
            txProcessingTime = transactionStatusFields.transactionProcessingTime,
            walletErrorType = transactionStatusFields.walletErrorType?.let { Json.decodeFromString(it) },
        )

        VALUE_STATUS_SUCCESS -> AppEvent.Status.Transaction.Success(
            requestId = checkNotNull(transactionStatusFields.requestId),
            transactionId = checkNotNull(transactionStatusFields.transactionId),
            isInternal = checkNotNull(transactionStatusFields.isInternal),
            blockUntilComplete = checkNotNull(transactionStatusFields.blockUntilComplete)
        )

        VALUE_STATUS_IN_PROGRESS -> AppEvent.Status.Transaction.InProgress(
            requestId = checkNotNull(transactionStatusFields.requestId),
            transactionId = checkNotNull(transactionStatusFields.transactionId),
            isInternal = checkNotNull(transactionStatusFields.isInternal),
            blockUntilComplete = checkNotNull(transactionStatusFields.blockUntilComplete)
        )

        else -> error("Status not received")
    }
}
