package com.babylon.wallet.android.presentation.status.assets.nonfungible

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import java.net.URLEncoder

private const val ROUTE = "non_fungible_asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_NEWLY_CREATED = "newly_created"
private const val ARG_SHOW_STAKE_CLAIM_BUTTON = "show_stake_claim"
private const val ARG_LOCAL_ID = "local_id"
private const val ARG_ACCOUNT_ADDRESS = "account_address"

fun NavController.nonFungibleAssetDialog(
    resourceAddress: String,
    localId: String? = null,
    isNewlyCreated: Boolean = false,
    accountAddress: String? = null,
    showStakeClaimButton: Boolean = true
) {
    val localIdParam = if (localId != null) "&$ARG_LOCAL_ID=${URLEncoder.encode(localId, Charsets.UTF_8.name())}" else ""
    val accountAddressParam = if (accountAddress != null) "&$ARG_ACCOUNT_ADDRESS=$accountAddress" else ""
    navigate(
        route = ROUTE +
            "?$ARG_RESOURCE_ADDRESS=$resourceAddress" +
            "&$ARG_NEWLY_CREATED=$isNewlyCreated" +
            "&$ARG_SHOW_STAKE_CLAIM_BUTTON=$showStakeClaimButton" +
            localIdParam +
            accountAddressParam
    )
}

internal class NonFungibleAssetDialogArgs(
    val resourceAddress: String,
    val isNewlyCreated: Boolean,
    val localId: String?,
    val accountAddress: String?,
    val showStakeClaimButton: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
        isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
        localId = savedStateHandle[ARG_LOCAL_ID],
        accountAddress = savedStateHandle[ARG_ACCOUNT_ADDRESS],
        showStakeClaimButton = requireNotNull(savedStateHandle[ARG_SHOW_STAKE_CLAIM_BUTTON])
    )
}

fun NavGraphBuilder.nonFungibleAssetDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = ROUTE +
            "?$ARG_RESOURCE_ADDRESS={$ARG_RESOURCE_ADDRESS}" +
            "&$ARG_NEWLY_CREATED={$ARG_NEWLY_CREATED}" +
            "&$ARG_SHOW_STAKE_CLAIM_BUTTON={$ARG_SHOW_STAKE_CLAIM_BUTTON}" +
            "&$ARG_LOCAL_ID={$ARG_LOCAL_ID}" +
            "&$ARG_ACCOUNT_ADDRESS={$ARG_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_RESOURCE_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_NEWLY_CREATED) {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument(ARG_LOCAL_ID) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_SHOW_STAKE_CLAIM_BUTTON) {
                type = NavType.BoolType
                defaultValue = true
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NonFungibleAssetDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
