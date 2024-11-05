package com.babylon.wallet.android.presentation.dialogs.assets

import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.data.gateway.generated.infrastructure.Serializer
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    resourceAddress: ResourceAddress,
    amounts: Map<ResourceAddress, CountedAmount> = emptyMap(),
    isNewlyCreated: Boolean = false,
    underAccountAddress: AccountAddress? = null,
) {
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=${underAccountAddress.string}" else ""
    val fungibleAmounts = Serializer.kotlinxSerializationJson.encodeToString(FungibleAmounts(amounts.mapKeys { it.key.string }))
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_FUNGIBLE" +
            "?${ARG_RESOURCE_ADDRESS}=${resourceAddress.string}" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            "&$ARG_AMOUNTS=$fungibleAmounts" +
            underAccountAddressParam
    )
}

fun NavController.nftAssetDialog(
    resourceAddress: ResourceAddress,
    localId: NonFungibleLocalId? = null,
    isNewlyCreated: Boolean = false,
    underAccountAddress: AccountAddress? = null
) {
    val localIdParam = if (localId != null) "&$ARG_LOCAL_ID=${URLEncoder.encode(localId.string, Charsets.UTF_8.name())}" else ""
    val underAccountAddressParam = if (underAccountAddress != null) "&$ARG_UNDER_ACCOUNT_ADDRESS=${underAccountAddress.string}" else ""
    navigate(
        route = "$ROUTE/$ARG_RESOURCE_TYPE_VALUE_NFT" +
            "?${ARG_RESOURCE_ADDRESS}=${resourceAddress.string}" +
            "&${ARG_NEWLY_CREATED}=$isNewlyCreated" +
            localIdParam +
            underAccountAddressParam
    )
}

@Serializable
private data class FungibleAmounts(
    val amounts: Map<String, @Contextual CountedAmount>
)

sealed interface AssetDialogArgs {
    val resourceAddress: ResourceAddress
    val isNewlyCreated: Boolean
    val underAccountAddress: AccountAddress?

    data class Fungible(
        override val resourceAddress: ResourceAddress,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: AccountAddress?,
        private val amounts: Map<String, CountedAmount>,
    ) : AssetDialogArgs {

        fun fungibleAmountOf(address: ResourceAddress): CountedAmount? = amounts[address.string]
    }

    data class NFT(
        override val resourceAddress: ResourceAddress,
        override val isNewlyCreated: Boolean,
        override val underAccountAddress: AccountAddress?,
        val localId: NonFungibleLocalId?
    ) : AssetDialogArgs

    companion object {
        fun from(savedStateHandle: SavedStateHandle): AssetDialogArgs {
            return when (requireNotNull(savedStateHandle[ARG_RESOURCE_TYPE])) {
                ARG_RESOURCE_TYPE_VALUE_FUNGIBLE -> {
                    val amountsSerialized = requireNotNull(savedStateHandle.get<String>(ARG_AMOUNTS))
                    val fungibleAmounts = Serializer.kotlinxSerializationJson.decodeFromString<FungibleAmounts>(amountsSerialized)
                    Fungible(
                        resourceAddress = ResourceAddress.init(requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS])),
                        isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                        underAccountAddress = savedStateHandle.get<String>(ARG_UNDER_ACCOUNT_ADDRESS)?.let { AccountAddress.init(it) },
                        amounts = fungibleAmounts.amounts
                    )
                }

                ARG_RESOURCE_TYPE_VALUE_NFT -> NFT(
                    resourceAddress = ResourceAddress.init(requireNotNull(savedStateHandle[ARG_RESOURCE_ADDRESS])),
                    isNewlyCreated = requireNotNull(savedStateHandle[ARG_NEWLY_CREATED]),
                    underAccountAddress = savedStateHandle.get<String>(ARG_UNDER_ACCOUNT_ADDRESS)?.let { AccountAddress.init(it) },
                    localId = savedStateHandle.get<String>(ARG_LOCAL_ID)?.let { NonFungibleLocalId.init(it) }
                )

                else -> error("No type specified.")
            }
        }
    }
}

fun NavGraphBuilder.assetDialog(
    onInfoClick: (GlossaryItem) -> Unit,
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
            onInfoClick = onInfoClick,
            onDismiss = onDismiss
        )
    }
}
