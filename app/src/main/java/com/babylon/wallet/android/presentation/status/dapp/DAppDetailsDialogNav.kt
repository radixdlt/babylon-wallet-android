package com.babylon.wallet.android.presentation.status.dapp

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

fun NavController.dAppDetailsDialog(
    dAppDefinitionAddress: AccountAddress
) {
    navigate(route = "$ROUTE/${dAppDefinitionAddress.string}")
}

internal class DAppDetailsDialogArgs(
    val dAppDefinitionAddress: AccountAddress
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        dAppDefinitionAddress = AccountAddress.init(requireNotNull(savedStateHandle[ARG_DAPP_DEFINITION_ADDRESS]))
    )
}

fun NavGraphBuilder.dAppDetailsDialog(
    onFungibleClick: (Resource.FungibleResource) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource) -> Unit,
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
            onFungibleClick = onFungibleClick,
            onNonFungibleClick = onNonFungibleClick,
            onDismiss = onDismiss
        )
    }
}
