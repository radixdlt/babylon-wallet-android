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
import com.babylon.wallet.android.domain.model.metadata.MetadataItem.Companion.consume
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.radixdlt.ret.Address
import com.radixdlt.ret.DecimalSource
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.NonFungibleLocalIdVecSource
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.ResourceTracker
import java.math.BigDecimal

typealias RETResources = com.radixdlt.ret.Resources
typealias RETResourcesAmount = com.radixdlt.ret.Resources.Amount
typealias RETResourcesIds = com.radixdlt.ret.Resources.Ids

fun RETResources.toTransferableResource(resourceAddress: String, allResources: List<Resources>): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()
    val allPoolUnits = allResources.map { resource ->
        resource.poolUnits.map { poolUnit ->
            poolUnit.poolUnitResource
        }
    }.flatten()

    return when (this) {
        is RETResourcesAmount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = (allFungibles + allPoolUnits).find { it.resourceAddress == resourceAddress }
                ?: Resource.FungibleResource(
                    resourceAddress = resourceAddress,
                    ownedAmount = BigDecimal.ZERO
                ),
            isNewlyCreated = false
        )

        is RETResourcesIds -> {
            val collection = allNFTCollections.find { it.resourceAddress == resourceAddress }?.let { collection ->
                collection.copy(
                    items = collection.items.filter { it.localId.toRetId() in ids }
                )
            } ?: Resource.NonFungibleResource(
                resourceAddress = resourceAddress,
                amount = ids.size.toLong(),
                items = ids.map { id ->
                    Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress,
                        localId = Resource.NonFungibleResource.Item.ID.from(id)
                    )
                }
            )

            TransferableResource.NFTs(
                resource = collection,
                isNewlyCreated = false
            )
        }
    }
}

fun ResourceTracker.toDepositingTransferableResource(
    allResources: List<Resources>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
    thirdPartyMetadata: Map<String, List<MetadataItem>>
): Transferable.Depositing {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()
    val allPoolUnits = allResources.map { resource ->
        resource.poolUnits.map { poolUnit ->
            poolUnit.poolUnitResource
        }
    }.flatten()

    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Depositing(
            transferable = toTransferableResource(
                allFungibles = allFungibles,
                allPoolUnits = allPoolUnits,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities,
                thirdPartyMetadata = thirdPartyMetadata
            ),
            guaranteeType = amount.toGuaranteeType()
        )

        is ResourceTracker.NonFungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(allNFTCollections, newlyCreatedMetadata, newlyCreatedEntities),
                guaranteeType = ids.toGuaranteeType()
            )
        }
    }
}

fun ResourceTracker.toWithdrawingTransferableResource(
    allResources: List<Resources>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>> = emptyMap(),
    newlyCreatedEntities: List<Address>
): Transferable.Withdrawing {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()
    val allPoolUnits = allResources.map { resource ->
        resource.poolUnits.map { poolUnit ->
            poolUnit.poolUnitResource
        }
    }.flatten()

    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                allFungibles = allFungibles,
                allPoolUnits = allPoolUnits,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            )
        )

        is ResourceTracker.NonFungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                allNFTCollections = allNFTCollections,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities,
            )
        )
    }
}

fun ResourceSpecifier.toTransferableResource(
    allResources: List<Resources>,
    newlyCreated: Map<String, Map<String, MetadataValue>> = emptyMap()
): TransferableResource {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()
    val allPoolUnits = allResources.map { resource ->
        resource.poolUnits.map { poolUnit ->
            poolUnit.poolUnitResource
        }
    }.flatten()

    return when (this) {
        is ResourceSpecifier.Amount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = (allFungibles + allPoolUnits).find {
                it.resourceAddress == resourceAddress.addressString()
            } ?: Resource.FungibleResource.from(
                resourceAddress = resourceAddress,
                metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
            ),
            isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
        )

        is ResourceSpecifier.Ids -> {
            val collection = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }
            val metadata = newlyCreated[resourceAddress.addressString()]
            val items = ids.map { id ->
                collection?.items?.find {
                    it.localId.toRetId() == id
                } ?: Resource.NonFungibleResource.Item(
                    collectionAddress = this.resourceAddress.addressString(),
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }
            TransferableResource.NFTs(
                resource = collection?.copy(
                    amount = ids.size.toLong(),
                    items = items
                ) ?: Resource.NonFungibleResource(
                    resourceAddress = resourceAddress.addressString(),
                    amount = ids.size.toLong(),
                    nameMetadataItem = metadata?.get(ExplicitMetadataKey.NAME.key)?.let { it as? MetadataValue.StringValue }?.let {
                        NameMetadataItem(name = it.value)
                    },
                    iconMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)?.let { it as? MetadataValue.StringValue }?.let {
                        IconUrlMetadataItem(url = Uri.parse(it.value))
                    },
                    items = items
                ),
                isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
            )
        }
    }
}

private fun Resource.FungibleResource.Companion.from(
    resourceAddress: Address,
    metadata: Map<String, MetadataValue?>
): Resource.FungibleResource = Resource.FungibleResource(
    resourceAddress = resourceAddress.addressString(),
    amount = BigDecimal.ZERO,
    nameMetadataItem = metadata[ExplicitMetadataKey.NAME.key]?.let { it as? MetadataValue.StringValue }?.let {
        NameMetadataItem(name = it.value)
    },
    symbolMetadataItem = metadata[ExplicitMetadataKey.SYMBOL.key]?.let { it as? MetadataValue.StringValue }?.let {
        SymbolMetadataItem(symbol = it.value)
    },
    descriptionMetadataItem = metadata[ExplicitMetadataKey.DESCRIPTION.key]?.let { it as? MetadataValue.StringValue }?.let {
        DescriptionMetadataItem(description = it.value)
    },
    iconUrlMetadataItem = metadata[ExplicitMetadataKey.ICON_URL.key]?.let { it as? MetadataValue.UrlValue }?.let {
        IconUrlMetadataItem(url = Uri.parse(it.value))
    }
)

private fun Resource.FungibleResource.Companion.from(
    resourceAddress: Address,
    metadataItems: List<MetadataItem>,
): Resource.FungibleResource = Resource.FungibleResource(
    resourceAddress = resourceAddress.addressString(),
    amount = BigDecimal.ZERO,
    nameMetadataItem = metadataItems.toMutableList().consume(),
    symbolMetadataItem = metadataItems.toMutableList().consume(),
    descriptionMetadataItem = metadataItems.toMutableList().consume(),
    iconUrlMetadataItem = metadataItems.toMutableList().consume()
)

private fun ResourceTracker.Fungible.toTransferableResource(
    allFungibles: List<Resource.FungibleResource>,
    allPoolUnits: List<Resource.FungibleResource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
    thirdPartyMetadata: Map<String, List<MetadataItem>> = emptyMap()
): TransferableResource.Amount {
    val resource = (allFungibles + allPoolUnits).find {
        it.resourceAddress == resourceAddress.addressString()
    } ?: if (thirdPartyMetadata[resourceAddress.addressString()] != null) {
        Resource.FungibleResource.from(
            resourceAddress = resourceAddress,
            metadataItems = thirdPartyMetadata[resourceAddress.addressString()].orEmpty()
        )
    } else {
        Resource.FungibleResource.from(
            resourceAddress = resourceAddress,
            metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
        )
    }

    return TransferableResource.Amount(
        amount = amount.valueDecimal,
        resource = resource,
        isNewlyCreated = newlyCreatedEntities.map { it.addressString() }.contains(resourceAddress.addressString())
    )
}

private fun ResourceTracker.NonFungible.toTransferableResource(
    allNFTCollections: List<Resource.NonFungibleResource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
): TransferableResource.NFTs {
    val collection = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }
    val metadata = newlyCreated[resourceAddress.addressString()]
    val items = ids.valueList.map { id ->
        collection?.items?.find {
            it.localId.toRetId() == id
        } ?: Resource.NonFungibleResource.Item(
            collectionAddress = this.resourceAddress.addressString(),
            localId = Resource.NonFungibleResource.Item.ID.from(id)
        )
    }

    return TransferableResource.NFTs(
        resource = collection?.copy(
            amount = items.size.toLong(),
            items = items
        ) ?: Resource.NonFungibleResource(
            resourceAddress = resourceAddress.addressString(),
            amount = items.size.toLong(),
            nameMetadataItem = metadata?.get(ExplicitMetadataKey.NAME.key)?.let { it as? MetadataValue.StringValue }?.let {
                NameMetadataItem(name = it.value)
            },
            iconMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)?.let { it as? MetadataValue.UrlValue }?.let {
                IconUrlMetadataItem(url = Uri.parse(it.value))
            },
            items = items
        ),
        isNewlyCreated = newlyCreatedEntities.map { it.addressString() }.contains(resourceAddress.addressString())
    )
}

private val DecimalSource.valueDecimal: BigDecimal
    get() = when (this) {
        is DecimalSource.Guaranteed -> value.asStr().toBigDecimal()
        is DecimalSource.Predicted -> value.asStr().toBigDecimal()
    }

private fun DecimalSource.toGuaranteeType(): GuaranteeType = when (this) {
    is DecimalSource.Guaranteed -> GuaranteeType.Guaranteed
    is DecimalSource.Predicted -> GuaranteeType.Predicted(instructionIndex = instructionIndex.toLong())
}

private val NonFungibleLocalIdVecSource.valueList: List<NonFungibleLocalId>
    get() = when (this) {
        is NonFungibleLocalIdVecSource.Guaranteed -> value
        is NonFungibleLocalIdVecSource.Predicted -> value
    }

private fun NonFungibleLocalIdVecSource.toGuaranteeType(): GuaranteeType = when (this) {
    is NonFungibleLocalIdVecSource.Guaranteed -> GuaranteeType.Guaranteed
    is NonFungibleLocalIdVecSource.Predicted -> GuaranteeType.Predicted(instructionIndex = instructionIndex.toLong())
}
