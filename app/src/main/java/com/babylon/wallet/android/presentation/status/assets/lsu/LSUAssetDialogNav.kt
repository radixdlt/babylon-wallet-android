package com.babylon.wallet.android.presentation.status.assets.lsu

import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

private const val ROUTE = "lsu_asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_ACCOUNT_ADDRESS = "account_address"

fun NavController.lsuAssetDialog(resourceAddress: String, accountAddress: String) {
    navigate(route = "$ROUTE?$ARG_RESOURCE_ADDRESS=$resourceAddress&$ARG_ACCOUNT_ADDRESS=$accountAddress")
}

internal class LSUAssetDialogArgs(
    val resourceAddress: String,
    val accountAddress: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
        accountAddress = requireNotNull(savedStateHandle[ARG_ACCOUNT_ADDRESS])
    )
}
fun NavGraphBuilder.lsuAssetDialog(
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
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LSUAssetDialog(onDismiss = onDismiss)
    }
}
