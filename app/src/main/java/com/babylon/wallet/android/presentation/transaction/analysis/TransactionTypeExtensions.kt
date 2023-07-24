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
import com.babylon.wallet.android.domain.model.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.metadata.SymbolMetadataItem
import com.radixdlt.ret.Address
import com.radixdlt.ret.DecimalSource
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.NonFungibleLocalIdVecSource
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.ResourceTracker
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
            ),
            isNewlyCreated = false
        )

        is RETResourcesIds -> {
            val collection = allNFTCollections.find { it.resourceAddress == resourceAddress }
            TransferableResource.NFTs(
                resource = collection ?: Resource.NonFungibleResource(
                    resourceAddress = resourceAddress,
                    amount = ids.size.toLong(),
                    items = ids.map { id ->
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceAddress,
                            localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
                        )
                    }
                ),
                isNewlyCreated = false
            )
        }
    }
}

fun ResourceTracker.toDepositingTransferableResource(
    allResources: List<Resources>,
    newlyCreated: Map<String, Map<String, MetadataValue>>
): Transferable.Depositing {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Depositing(
            transferable = toTransferableResource(allFungibles, newlyCreated),
            guaranteeType = amount.toGuaranteeType()
        )

        is ResourceTracker.NonFungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(allNFTCollections, newlyCreated),
                guaranteeType = ids.toGuaranteeType()
            )
        }
    }
}

fun ResourceTracker.toWithdrawingTransferableResource(
    allResources: List<Resources>,
    newlyCreated: Map<String, Map<String, MetadataValue>> = emptyMap()
): Transferable.Withdrawing {
    val allFungibles = allResources.map { it.fungibleResources }.flatten()
    val allNFTCollections = allResources.map { it.nonFungibleResources }.flatten()

    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(allFungibles, newlyCreated)
        )

        is ResourceTracker.NonFungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(allNFTCollections, newlyCreated)
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
            resource = allFungibles.find {
                it.resourceAddress == resourceAddress.addressString()
            } ?: Resource.FungibleResource.from(
                resourceAddress = resourceAddress,
                metadata = newlyCreated[resourceAddress.addressString()]
            ),
            isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
        )

        is ResourceSpecifier.Ids -> {
            val collection = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }
            val metadata = newlyCreated[resourceAddress.addressString()]
            val items = ids.map { id ->
                collection?.items?.find {
                    it.localId == id
                } ?: Resource.NonFungibleResource.Item(
                    collectionAddress = this.resourceAddress.addressString(),
                    localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
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

private fun ResourceTracker.Fungible.toTransferableResource(
    allFungibles: List<Resource.FungibleResource>,
    newlyCreated: Map<String, Map<String, MetadataValue>>
) = TransferableResource.Amount(
    amount = amount.valueDecimal,
    resource = allFungibles.find {
        it.resourceAddress == resourceAddress.addressString()
    } ?: Resource.FungibleResource.from(
        resourceAddress = resourceAddress,
        metadata = newlyCreated[resourceAddress.addressString()]
    ),
    isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
)

private fun ResourceTracker.NonFungible.toTransferableResource(
    allNFTCollections: List<Resource.NonFungibleResource>,
    newlyCreated: Map<String, Map<String, MetadataValue>>
): TransferableResource.NFTs {
    val collection = allNFTCollections.find { it.resourceAddress == resourceAddress.addressString() }
    val metadata = newlyCreated[resourceAddress.addressString()]
    val items = ids.valueList.map { id ->
        collection?.items?.find {
            it.localId == id
        } ?: Resource.NonFungibleResource.Item(
            collectionAddress = this.resourceAddress.addressString(),
            localId = Resource.NonFungibleResource.Item.ID.from(id.asStr())
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
            iconMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)?.let { it as? MetadataValue.StringValue }?.let {
                IconUrlMetadataItem(url = Uri.parse(it.value))
            },
            items = items
        ),
        isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
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
