package com.babylon.wallet.android.presentation.status.assets.fungible

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

private const val ROUTE = "fungible_asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_ACCOUNT_ADDRESS = "account_address"

fun NavController.fungibleAssetDialog(resourceAddress: String, accountAddress: String? = null) {
    val accountAddressParam = if (accountAddress != null) "&$ARG_ACCOUNT_ADDRESS=$accountAddress" else ""
    navigate(route = "$ROUTE?$ARG_RESOURCE_ADDRESS=$resourceAddress$accountAddressParam")
}

internal class FungibleAssetDialogArgs(
    val resourceAddress: String,
    val accountAddress: String?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
        accountAddress = savedStateHandle[ARG_ACCOUNT_ADDRESS]
    )
}
fun NavGraphBuilder.fungibleAssetDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE?$ARG_RESOURCE_ADDRESS={$ARG_RESOURCE_ADDRESS}&$ARG_ACCOUNT_ADDRESS={$ARG_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_RESOURCE_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        FungibleAssetDialog(onDismiss = onDismiss)
    }
}
