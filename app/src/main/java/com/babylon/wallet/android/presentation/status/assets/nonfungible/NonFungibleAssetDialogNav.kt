package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

private const val ROUTE = "non_fungible_asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_LOCAL_ID = "local_id"

fun NavController.nonFungibleAssetDialog(resourceAddress: String, localId: String? = null) {
    val localIdParam = if (localId != null) "&$ARG_LOCAL_ID=$localId" else ""
    navigate(route = "$ROUTE?$ARG_RESOURCE_ADDRESS=$resourceAddress$localIdParam")
}

internal class NonFungibleAssetDialogArgs(
    val resourceAddress: String,
    val localId: String?
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
        localId = savedStateHandle[ARG_LOCAL_ID]
    )
}
fun NavGraphBuilder.nonFungibleAssetDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE?$ARG_RESOURCE_ADDRESS={$ARG_RESOURCE_ADDRESS}&$ARG_LOCAL_ID={$ARG_LOCAL_ID}",
        arguments = listOf(
            navArgument(ARG_RESOURCE_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_LOCAL_ID) {
                type = NavType.StringType
                nullable = true
            }
        )
    ) {
        NonFungibleAssetDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
