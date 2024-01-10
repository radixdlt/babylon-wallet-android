package com.babylon.wallet.android.presentation.status.dapp

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

private const val ROUTE = "dApp_details_dialog"
private const val ARG_DAPP_DEFINITION_ADDRESS = "dApp_definition_address"

fun NavController.dAppDetailsDialog(
    dAppDefinitionAddress: String
) {
    navigate(route = "$ROUTE/$dAppDefinitionAddress")
}

internal class DAppDetailsDialogArgs(
    val dAppDefinitionAddress: String
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        dAppDefinitionAddress = requireNotNull(savedStateHandle[ARG_DAPP_DEFINITION_ADDRESS])
    )
}

fun NavGraphBuilder.dAppDetailsDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_DAPP_DEFINITION_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_DAPP_DEFINITION_ADDRESS) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DAppDetailsDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
