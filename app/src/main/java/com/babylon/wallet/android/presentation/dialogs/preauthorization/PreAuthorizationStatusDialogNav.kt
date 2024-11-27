package com.babylon.wallet.android.presentation.dialogs.preauthorization

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.presentation.transaction.ROUTE_TRANSACTION_REVIEW
import com.babylon.wallet.android.utils.AppEvent
import com.radixdlt.sargon.SubintentHash
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.extensions.init
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

private const val VALUE_STATUS_SUCCESS = "success"
private const val VALUE_STATUS_EXPIRED = "expired"
private const val VALUE_STATUS_SENT = "sent"
private const val ROUTE = "pre_authorization_status_dialog"
private const val ARG_PRE_AUTHORIZATION_STATUS_FIELDS = "arg_pre_authorization_status_fields"

@Keep
@Serializable
@Parcelize
data class PreAuthorizationStatusFields(
    val status: String,
    val requestId: String,
    val preAuthorizationId: String,
    val isMobileConnect: Boolean,
    val dAppName: String?,
    val transactionId: String?,
    val remainingTime: Long? = null
) : Parcelable

val PreAuthorizationStatusParameterType = object : NavType<PreAuthorizationStatusFields>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): PreAuthorizationStatusFields? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, PreAuthorizationStatusFields::class.java)
        } else {
            bundle.getParcelable(key)
        }
    }

    override fun parseValue(value: String): PreAuthorizationStatusFields {
        return Serializer.kotlinxSerializationJson.decodeFromString(value)
    }

    override fun put(bundle: Bundle, key: String, value: PreAuthorizationStatusFields) {
        bundle.putParcelable(key, value)
    }
}

internal class PreAuthorizationStatusDialogArgs(
    val event: AppEvent.Status.PreAuthorization
) {
    constructor(savedStateHandle: SavedStateHandle) : this(savedStateHandle.toStatus())
}

fun NavController.preAuthorizationStatusDialog(transactionEvent: AppEvent.Status.PreAuthorization) {
    // Do not create another entry when this dialog exists
    // New requests will be handled from the view model itself
    if (currentBackStackEntry?.destination?.route?.startsWith(ROUTE) == true) return

    val argument = Json.encodeToString(transactionEvent.toStatusFields())
    navigate("$ROUTE/$argument") {
        popUpTo(route = ROUTE_TRANSACTION_REVIEW) {
            inclusive = true
        }
    }
}

fun NavGraphBuilder.preAuthorizationStatusDialog(
    onClose: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_PRE_AUTHORIZATION_STATUS_FIELDS}",
        arguments = listOf(
            navArgument(ARG_PRE_AUTHORIZATION_STATUS_FIELDS) {
                type = PreAuthorizationStatusParameterType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PreAuthorizationStatusDialog(
            viewModel = hiltViewModel(),
            onDismiss = onClose
        )
    }
}

private fun AppEvent.Status.PreAuthorization.toStatus() = when (this) {
    is AppEvent.Status.PreAuthorization.Expired -> VALUE_STATUS_EXPIRED
    is AppEvent.Status.PreAuthorization.Sent -> VALUE_STATUS_SENT
    is AppEvent.Status.PreAuthorization.Success -> VALUE_STATUS_SUCCESS
}

fun AppEvent.Status.PreAuthorization.toStatusFields(): PreAuthorizationStatusFields {
    val requestId = requestId.ifBlank { error("Request id cannot be empty") }

    return PreAuthorizationStatusFields(
        status = toStatus(),
        requestId = requestId,
        preAuthorizationId = encodedPreAuthorizationId,
        isMobileConnect = isMobileConnect,
        dAppName = dAppName,
        transactionId = when (this) {
            is AppEvent.Status.PreAuthorization.Success -> transactionId.bech32EncodedTxId
            else -> null
        },
        remainingTime = when (this) {
            is AppEvent.Status.PreAuthorization.Sent -> remainingTime.inWholeSeconds
            else -> null
        }
    )
}

private fun SavedStateHandle.toStatus(): AppEvent.Status.PreAuthorization {
    val statusFields = checkNotNull(get<PreAuthorizationStatusFields>(ARG_PRE_AUTHORIZATION_STATUS_FIELDS))
    return when (checkNotNull(statusFields.status)) {
        VALUE_STATUS_EXPIRED -> AppEvent.Status.PreAuthorization.Expired(
            requestId = checkNotNull(statusFields.requestId),
            preAuthorizationId = SubintentHash.init(checkNotNull(statusFields.preAuthorizationId)),
            isMobileConnect = checkNotNull(statusFields.isMobileConnect),
            dAppName = statusFields.dAppName
        )

        VALUE_STATUS_SUCCESS -> AppEvent.Status.PreAuthorization.Success(
            requestId = checkNotNull(statusFields.requestId),
            preAuthorizationId = SubintentHash.init(checkNotNull(statusFields.preAuthorizationId)),
            isMobileConnect = checkNotNull(statusFields.isMobileConnect),
            dAppName = statusFields.dAppName,
            transactionId = TransactionIntentHash.init(checkNotNull(statusFields.transactionId))
        )

        VALUE_STATUS_SENT -> AppEvent.Status.PreAuthorization.Sent(
            requestId = checkNotNull(statusFields.requestId),
            preAuthorizationId = SubintentHash.init(checkNotNull(statusFields.preAuthorizationId)),
            isMobileConnect = checkNotNull(statusFields.isMobileConnect),
            dAppName = statusFields.dAppName,
            remainingTime = checkNotNull(statusFields.remainingTime?.seconds)
        )

        else -> error("Status not received")
    }
}
