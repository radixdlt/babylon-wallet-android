package com.babylon.wallet.android.presentation.dialogs.dapp

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.resources.Resource

private const val ROUTE = "dApp_details_dialog"
private const val ARG_DAPP_DEFINITION_ADDRESS = "dApp_definition_address"
private const val ARG_READ_ONLY = "read_only"

fun NavController.dAppDetailsDialog(
    dAppDefinitionAddress: AccountAddress,
    isReadOnly: Boolean = false
) {
    navigate(route = "$ROUTE/${dAppDefinitionAddress.string}/$isReadOnly")
}

internal class DAppDetailsDialogArgs(
    val dAppDefinitionAddress: AccountAddress,
    val isReadOnly: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        dAppDefinitionAddress = AccountAddress.init(requireNotNull(savedStateHandle[ARG_DAPP_DEFINITION_ADDRESS])),
        isReadOnly = savedStateHandle[ARG_READ_ONLY] ?: false
    )
}

fun NavGraphBuilder.dAppDetailsDialog(
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_DAPP_DEFINITION_ADDRESS}/{$ARG_READ_ONLY}",
        arguments = listOf(
            navArgument(ARG_DAPP_DEFINITION_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_READ_ONLY) {
                type = NavType.BoolType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DAppDetailsDialog(
            viewModel = hiltViewModel(),
            onFungibleClick = onFungibleClick,
            onNonFungibleClick = onNonFungibleClick,
            onDismiss = onDismiss
        )
    }
}
