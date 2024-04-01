package com.babylon.wallet.android.presentation.status.assets

import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.math.BigDecimal
import java.net.URLEncoder

private const val ROUTE = "asset_dialog"
private const val ARG_RESOURCE_ADDRESS = "resource_address"
private const val ARG_NEWLY_CREATED = "newly_created"
private const val ARG_AMOUNTS = "amounts"
private const val ARG_LOCAL_ID = "local_id"
private const val ARG_UNDER_ACCOUNT_ADDRESS = "under_account_address"

private const val ARG_RESOURCE_TYPE = "resource_type"
private const val ARG_RESOURCE_TYPE_VALUE_FUNGIBLE = "fungible"
private const val ARG_RESOURCE_TYPE_VALUE_NFT = "nft"

fun NavController.fungibleAssetDialog(
    resourceAddress: String,
    amounts: Map<String, BigDecimal> = emptyMap(),
    isNewlyCreated: Boolean = false,
    underAccountAddress: String? = null,
) {
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=$underAccountAddress" else ""
    val fungibleAmounts = Serializer.kotlinxSerializationJson.encodeToString(FungibleAmounts(amounts))
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_FUNGIBLE" +
            "?${ARG_RESOURCE_ADDRESS}=$resourceAddress" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            "&$ARG_AMOUNTS=$fungibleAmounts" +
            underAccountAddressParam
    )
}

fun NavController.nftAssetDialog(
    resourceAddress: String,
    localId: NonFungibleLocalId? = null,
    isNewlyCreated: Boolean = false,
    underAccountAddress: String? = null
) {
    val localIdParam = if (localId != null) "&$ARG_LOCAL_ID=${URLEncoder.encode(localId.string, Charsets.UTF_8.name())}" else ""
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=$underAccountAddress" else ""
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_NFT" +
            "?${ARG_RESOURCE_ADDRESS}=$resourceAddress" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            localIdParam +
            underAccountAddressParam
    )
}

@Serializable
private data class FungibleAmounts(
    val amounts: Map<String, @Contextual BigDecimal>
)

sealed interface AssetDialogArgs {
    val resourceAddress: String
    val isNewlyCreated: Boolean
    val underAccountAddress: String?

    data class Fungible(
        override val resourceAddress: String,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: String?,
        private val amounts: Map<String, BigDecimal>,
    ) : AssetDialogArgs {

        fun fungibleAmountOf(address: String): BigDecimal? = amounts[address]
    }

    data class NFT(
        override val resourceAddress: String,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: String?,
        val localId: NonFungibleLocalId?
    ) : AssetDialogArgs

    companion object {
        fun from(savedStateHandle: SavedStateHandle): AssetDialogArgs {
            return when (requireNotNull(savedStateHandle[ARG_RESOURCE_TYPE])) {
                ARG_RESOURCE_TYPE_VALUE_FUNGIBLE -> {
                    val amountsSerialized = requireNotNull(savedStateHandle.get<String>(ARG_AMOUNTS))
                    val fungibleAmounts = Serializer.kotlinxSerializationJson.decodeFromString<FungibleAmounts>(amountsSerialized)
                    Fungible(
                        resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
                        isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                        underAccountAddress = savedStateHandle[ARG_UNDER_ACCOUNT_ADDRESS],
                        amounts = fungibleAmounts.amounts
                    )
                }

                ARG_RESOURCE_TYPE_VALUE_NFT -> NFT(
                    resourceAddress = requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS]),
                    isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                    underAccountAddress = savedStateHandle[ARG_UNDER_ACCOUNT_ADDRESS],
                    localId = savedStateHandle.get<String>(ARG_LOCAL_ID)?.let { NonFungibleLocalId.init(it) }
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
            "&$ARG_AMOUNTS={$ARG_AMOUNTS}" +
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
            navArgument(ARG_AMOUNTS) {
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
