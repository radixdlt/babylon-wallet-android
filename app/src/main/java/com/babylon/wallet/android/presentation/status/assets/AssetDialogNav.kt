package com.babylon.wallet.android.presentation.status.assets

import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import java.math.BigDecimal
import java.net.URLEncoder

private const val ROUTE = "asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_NEWLY_CREATED = "newly_created"
private const val ARG_AMOUNT = "amount"
private const val ARG_LOCAL_ID = "local_id"
private const val ARG_UNDER_ACCOUNT_ADDRESS = "under_account_address"

private const val ARG_RESOURCE_TYPE = "resource_type"
private const val ARG_RESOURCE_TYPE_VALUE_FUNGIBLE = "fungible"
private const val ARG_RESOURCE_TYPE_VALUE_NFT = "nft"

fun NavController.fungibleAssetDialog(
    resourceAddress: String,
    amount: BigDecimal? = null,
    isNewlyCreated: Boolean = false,
    underAccountAddress: String? = null
) {
    val amountParam = if (amount != null) "&$ARG_AMOUNT=${amount.toPlainString()}" else ""
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=$underAccountAddress" else ""
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_FUNGIBLE" +
            "?${ARG_RESOURCE_ADDRESS}=$resourceAddress" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            amountParam +
            underAccountAddressParam
    )
}

fun NavController.nftAssetDialog(
    resourceAddress: String,
    localId: String? = null,
    isNewlyCreated: Boolean = false,
    underAccountAddress: String? = null
) {
    val localIdParam = if (localId != null) "&$ARG_LOCAL_ID=${URLEncoder.encode(localId, Charsets.UTF_8.name())}" else ""
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=$underAccountAddress" else ""
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_NFT" +
            "?${ARG_RESOURCE_ADDRESS}=$resourceAddress" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            localIdParam +
            underAccountAddressParam
    )
}

sealed interface AssetDialogArgs {
    val resourceAddress: String
    val isNewlyCreated: Boolean
    val underAccountAddress: String?

    val isAmountPresent: Boolean
        get() = (this as? Fungible)?.amount != null

    data class Fungible(
        override val resourceAddress: String,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: String?,
        val amount: BigDecimal?,
    ) : AssetDialogArgs

    data class NFT(
        override val resourceAddress: String,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: String?,
        val localId: String?
    ) : AssetDialogArgs

    companion object {
        fun from(savedStateHandle: SavedStateHandle): AssetDialogArgs {
            return when (requireNotNull(savedStateHandle[ARG_RESOURCE_TYPE])) {
                ARG_RESOURCE_TYPE_VALUE_FUNGIBLE -> Fungible(
                    resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
                    isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                    underAccountAddress = savedStateHandle[ARG_UNDER_ACCOUNT_ADDRESS],
                    amount = savedStateHandle.get<String>(ARG_AMOUNT)?.toBigDecimalOrNull()
                )
                ARG_RESOURCE_TYPE_VALUE_NFT -> NFT(
                    resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
                    isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                    underAccountAddress = savedStateHandle[ARG_UNDER_ACCOUNT_ADDRESS],
                    localId = savedStateHandle[ARG_LOCAL_ID]
                )
                else -> error("No type specified.")
            }
        }
    }
}

fun NavGraphBuilder.assetDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_RESOURCE_TYPE}" +
            "?$ARG_RESOURCE_ADDRESS={$ARG_RESOURCE_ADDRESS}" +
            "&$ARG_NEWLY_CREATED={$ARG_NEWLY_CREATED}" +
            "&$ARG_AMOUNT={$ARG_AMOUNT}" +
            "&$ARG_LOCAL_ID={$ARG_LOCAL_ID}" +
            "&$ARG_UNDER_ACCOUNT_ADDRESS={$ARG_UNDER_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_RESOURCE_TYPE) {
                type = NavType.StringType
            },
            navArgument(ARG_RESOURCE_ADDRESS) {
                type = NavType.StringType
            },
            navArgument(ARG_NEWLY_CREATED) {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument(ARG_RESOURCE_TYPE) {
                type = NavType.StringType
            },
            navArgument(ARG_AMOUNT) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_LOCAL_ID) {
                type = NavType.StringType
                nullable = true
            },
            navArgument(ARG_UNDER_ACCOUNT_ADDRESS) {
                type = NavType.StringType
                nullable = true
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AssetDialog(
            onDismiss = onDismiss
        )
    }
}