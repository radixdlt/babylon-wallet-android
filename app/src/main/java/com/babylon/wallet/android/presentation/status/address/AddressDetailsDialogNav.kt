package com.babylon.wallet.android.presentation.status.address

import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddress
import com.babylon.wallet.android.utils.decodeUtf8
import com.babylon.wallet.android.utils.encodeUtf8
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private const val ROUTE = "address_details"
private const val ARG_ACTIONABLE_ADDRESS = "actionable_address"

internal class AddressDetailsArgs(
    val actionableAddress: ActionableAddress
) {

    constructor(savedStateHandle: SavedStateHandle): this(
        actionableAddress = checkNotNull(savedStateHandle.get<String>(ARG_ACTIONABLE_ADDRESS)).let {
            Json.decodeFromString(it.decodeUtf8())
        }
    )

}

fun NavController.addressDetails(
    actionableAddress: ActionableAddress
) {
    val addressSerialized = Json.encodeToString(actionableAddress).encodeUtf8()
    navigate(route = "$ROUTE?$ARG_ACTIONABLE_ADDRESS=$addressSerialized")
}

fun NavGraphBuilder.addressDetails(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE?$ARG_ACTIONABLE_ADDRESS={$ARG_ACTIONABLE_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ACTIONABLE_ADDRESS) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AddressDetailsDialog(
            onDismiss = onDismiss
        )
    }
}