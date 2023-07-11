package com.babylon.wallet.android.presentation.transaction.analysis

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Resource
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.StringMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.radixdlt.ret.Address
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.Source
import rdx.works.core.ret.asStr
import java.math.BigDecimal

typealias RETResources = com.radixdlt.ret.Resources
typealias RETResourcesAmount = com.radixdlt.ret.Resources.Amount
typealias RETResourcesIds = com.radixdlt.ret.Resources.Ids

fun RETResources.toTransferableResource(resourceAddress: String, allResources: List<Resources>): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is RETResourcesAmount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = allFungibles.find { it.resourceAddress == resourceAddress } ?: Resource.FungibleResource(
                resourceAddress = resourceAddress,
                amount = BigDecimal.ZERO
            )
        )
        is RETResourcesIds -> {
            val collectionItems = allNFTCollections.find { it.resourceAddress == resourceAddress }?.items.orEmpty()
            TransferableResource.NFTs(
                items = ids.map { id ->
                    collectionItems.find { it.localId == id } ?: Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress,
                        localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
                    )
                }
            )
        }
    }
}


fun Source.toDepositingTransferableResource(
    allResources: List<Resources>,
    newlyCreated: Map<String, Map<String, MetadataValue>>
): Transferable.Depositing {
    return when (this) {
        is Source.Guaranteed -> Transferable.Depositing(
            transferable = value.toTransferableResource(allResources, newlyCreated),
            guaranteeType = GuaranteeType.Guaranteed
        )
        is Source.Predicted -> Transferable.Depositing(
            transferable = value.toTransferableResource(allResources, newlyCreated),
            guaranteeType = GuaranteeType.Predicted(instructionIndex = instructionIndex.toLong())
        )
    }
}

fun ResourceSpecifier.toTransferableResource(
    allResources: List<Resources>,
    newlyCreated: Map<String, Map<String, MetadataValue>> = emptyMap()
): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is ResourceSpecifier.Amount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource =  allFungibles.find {
                it.resourceAddress == resourceAddress.addressString()
            } ?: Resource.FungibleResource.from(
                resourceAddress = resourceAddress,
                metadata = newlyCreated[resourceAddress.addressString()]
            )
        )
        is ResourceSpecifier.Ids -> {
            val collectionItems = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }?.items.orEmpty()
            TransferableResource.NFTs(
                items = ids.map { id ->
                    collectionItems.find {
                        it.localId == id
                    } ?: Resource.NonFungibleResource.Item.from(
                        collectionAddress = this.resourceAddress,
                        localId = id,
                        metadata = newlyCreated[resourceAddress.addressString()]
                    )
                }
            )
        }
    }
}

private fun Resource.FungibleResource.Companion.from(
    resourceAddress: Address,
    metadata: Map<String, MetadataValue>?
): Resource.FungibleResource = Resource.FungibleResource(
    resourceAddress = resourceAddress.addressString(),
    amount = BigDecimal.ZERO,
    nameMetadataItem = metadata?.get(ExplicitMetadataKey.NAME.key)?.let { it as? MetadataValue.StringValue }?.let {
        NameMetadataItem(name = it.value)
    },
    symbolMetadataItem = metadata?.get(ExplicitMetadataKey.SYMBOL.key)?.let { it as? MetadataValue.StringValue }?.let {
        SymbolMetadataItem(symbol = it.value)
    },
    descriptionMetadataItem = metadata?.get(ExplicitMetadataKey.DESCRIPTION.key)?.let { it as? MetadataValue.StringValue }?.let {
        DescriptionMetadataItem(description = it.value)
    },
    iconUrlMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)?.let { it as? MetadataValue.StringValue }?.let {
        IconUrlMetadataItem(url = Uri.parse(it.value))
    }
)

private fun Resource.NonFungibleResource.Item.Companion.from(
    collectionAddress: Address,
    localId: NonFungibleLocalId,
    metadata: Map<String, MetadataValue>?
): Resource.NonFungibleResource.Item = Resource.NonFungibleResource.Item(
    collectionAddress = collectionAddress.addressString(),
    localId = Resource.NonFungibleResource.Item.ID.from(localId.asStr()),
    nameMetadataItem = metadata?.get(ExplicitMetadataKey.NAME.key)?.let { it as? MetadataValue.StringValue }?.let {
        NameMetadataItem(name = it.value)
    },
    iconMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)?.let { it as? MetadataValue.StringValue }?.let {
        IconUrlMetadataItem(url = Uri.parse(it.value))
    }
)
