@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis

import android.net.Uri
import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.DescriptionMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.IconUrlMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.NameMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.SymbolMetadataItem
import com.babylon.wallet.android.domain.model.resources.metadata.TagsMetadataItem
import com.radixdlt.ret.Address
import com.radixdlt.ret.AuthorizedDepositorsChanges
import com.radixdlt.ret.DecimalSource
import com.radixdlt.ret.MetadataValue
import com.radixdlt.ret.NonFungibleLocalId
import com.radixdlt.ret.NonFungibleLocalIdVecSource
import com.radixdlt.ret.ResourceOrNonFungible
import com.radixdlt.ret.ResourceSpecifier
import com.radixdlt.ret.ResourceTracker
import com.radixdlt.ret.TransactionType
import java.math.BigDecimal

typealias RETResources = com.radixdlt.ret.Resources
typealias RETResourcesAmount = com.radixdlt.ret.Resources.Amount
typealias RETResourcesIds = com.radixdlt.ret.Resources.Ids

val ResourceTracker.resourceAddress: String
    get() = when (this) {
        is ResourceTracker.Fungible -> resourceAddress.addressString()
        is ResourceTracker.NonFungible -> resourceAddress.addressString()
    }

val ResourceSpecifier.resourceAddress: String
    get() = when (this) {
        is ResourceSpecifier.Amount -> resourceAddress.addressString()
        is ResourceSpecifier.Ids -> resourceAddress.addressString()
    }

val ResourceOrNonFungible.resourceAddress: String
    get() = when (this) {
        is ResourceOrNonFungible.NonFungible -> value.resourceAddress().addressString()
        is ResourceOrNonFungible.Resource -> value.addressString()
    }

val AuthorizedDepositorsChanges.resourceAddresses: Set<String>
    get() = added.map { it.resourceAddress }.toSet() union removed.map { it.resourceAddress }.toSet()

val TransactionType.involvedResourceAddresses: Set<String>
    get() = when (this) {
        is TransactionType.GeneralTransaction -> accountWithdraws
            .values
            .flatten()
            .map { it.resourceAddress }
            .toSet() union accountDeposits
            .values
            .flatten()
            .map { it.resourceAddress }
            .toSet()

        is TransactionType.SimpleTransfer -> setOf(transferred.resourceAddress)
        is TransactionType.Transfer -> transfers
            .map { it.value.keys }
            .flatten()
            .toSet()

        is TransactionType.AccountDepositSettings -> resourcePreferenceChanges
            .values
            .map { it.keys }
            .flatten()
            .toSet() union authorizedDepositorsChanges
            .map { it.value.resourceAddresses }
            .flatten()

        // TODO currently unavailable preview
        is TransactionType.ClaimStakeTransaction -> emptySet()
        is TransactionType.UnstakeTransaction -> emptySet()
        is TransactionType.StakeTransaction -> emptySet()
    }

fun RETResources.toTransferableResource(resourceAddress: String, resources: List<Resource>): TransferableResource {
    return when (this) {
        is RETResourcesAmount -> {
            val resource = resources.findFungible(resourceAddress) ?: Resource.FungibleResource(
                resourceAddress = resourceAddress,
                ownedAmount = BigDecimal.ZERO
            )
            TransferableResource.Amount(
                amount = amount.asStr().toBigDecimal(),
                resource = resource,
                isNewlyCreated = false
            )
        }

        is RETResourcesIds -> {
            val items = ids.map { id ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = resourceAddress,
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }

            val collection = resources.findNonFungible(resourceAddress)?.copy(items = items) ?: Resource.NonFungibleResource(
                resourceAddress = resourceAddress,
                amount = ids.size.toLong(),
                items = items
            )

            TransferableResource.NFTs(
                resource = collection,
                isNewlyCreated = false
            )
        }
    }
}

fun ResourceTracker.toDepositingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>,
    defaultDepositGuarantees: Double
): Transferable.Depositing {
    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Depositing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            ),
            guaranteeType = amount.toGuaranteeType(defaultDepositGuarantees)
        )

        is ResourceTracker.NonFungible -> {
            Transferable.Depositing(
                transferable = toTransferableResource(
                    resources = resources,
                    newlyCreated = newlyCreatedMetadata,
                    newlyCreatedEntities = newlyCreatedEntities
                ),
                guaranteeType = ids.toGuaranteeType(defaultDepositGuarantees)
            )
        }
    }
}

fun ResourceTracker.toWithdrawingTransferableResource(
    resources: List<Resource>,
    newlyCreatedMetadata: Map<String, Map<String, MetadataValue?>> = emptyMap(),
    newlyCreatedEntities: List<Address>
): Transferable.Withdrawing {
    return when (this) {
        is ResourceTracker.Fungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities
            )
        )

        is ResourceTracker.NonFungible -> Transferable.Withdrawing(
            transferable = toTransferableResource(
                resources = resources,
                newlyCreated = newlyCreatedMetadata,
                newlyCreatedEntities = newlyCreatedEntities,
            )
        )
    }
}

fun ResourceSpecifier.toTransferableResource(
    resources: List<Resource>,
    newlyCreated: Map<String, Map<String, MetadataValue>> = emptyMap()
): TransferableResource {
    return when (this) {
        is ResourceSpecifier.Amount -> TransferableResource.Amount(
            amount = amount.asStr().toBigDecimal(),
            resource = resources.findFungible(resourceAddress.addressString()) ?: Resource.FungibleResource.from(
                resourceAddress = resourceAddress,
                metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
            ),
            isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
        )

        is ResourceSpecifier.Ids -> {
            val metadata = newlyCreated[resourceAddress.addressString()]
            val items = ids.map { id ->
                Resource.NonFungibleResource.Item(
                    collectionAddress = this.resourceAddress.addressString(),
                    localId = Resource.NonFungibleResource.Item.ID.from(id)
                )
            }
            TransferableResource.NFTs(
                resource = resources.findNonFungible(resourceAddress.addressString())?.copy(
                    items = items
                ) ?: Resource.NonFungibleResource(
                    resourceAddress = resourceAddress.addressString(),
                    amount = ids.size.toLong(),
                    nameMetadataItem = metadata?.get(ExplicitMetadataKey.NAME.key)
                        ?.let { it as? MetadataValue.StringValue }?.let {
                            NameMetadataItem(name = it.value)
                        },
                    iconMetadataItem = metadata?.get(ExplicitMetadataKey.ICON_URL.key)
                        ?.let { it as? MetadataValue.UrlValue }?.let {
                            IconUrlMetadataItem(url = Uri.parse(it.value))
                        },
                    items = items
                ),
                isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
            )
        }
    }
}

private fun ResourceTracker.Fungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableResource.Amount {
    val resource = resources.findFungible(
        resourceAddress.addressString()
    ) ?: Resource.FungibleResource.from(
        resourceAddress = resourceAddress,
        metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
    )

    return TransferableResource.Amount(
        amount = amount.valueDecimal,
        resource = resource,
        isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
    )
}

private fun Resource.FungibleResource.Companion.from(
    resourceAddress: Address,
    metadata: Map<String, MetadataValue?>
): Resource.FungibleResource = Resource.FungibleResource(
    resourceAddress = resourceAddress.addressString(),
    ownedAmount = BigDecimal.ZERO,
    nameMetadataItem = metadata[ExplicitMetadataKey.NAME.key]?.let { it as? MetadataValue.StringValue }?.let {
        NameMetadataItem(name = it.value)
    },
    symbolMetadataItem = metadata[ExplicitMetadataKey.SYMBOL.key]?.let { it as? MetadataValue.StringValue }?.let {
        SymbolMetadataItem(symbol = it.value)
    },
    descriptionMetadataItem = metadata[ExplicitMetadataKey.DESCRIPTION.key]?.let { it as? MetadataValue.StringValue }
        ?.let {
            DescriptionMetadataItem(description = it.value)
        },
    iconUrlMetadataItem = metadata[ExplicitMetadataKey.ICON_URL.key]?.let { it as? MetadataValue.UrlValue }?.let {
        IconUrlMetadataItem(url = Uri.parse(it.value))
    },
    tagsMetadataItem = metadata[ExplicitMetadataKey.TAGS.key]?.let { it as MetadataValue.StringArrayValue }?.let {
        TagsMetadataItem(tags = it.value)
    }
)

private fun ResourceTracker.NonFungible.toTransferableResource(
    resources: List<Resource>,
    newlyCreated: Map<String, Map<String, MetadataValue?>>,
    newlyCreatedEntities: List<Address>
): TransferableResource.NFTs {
    val items = ids.valueList.map { id ->
        Resource.NonFungibleResource.Item(
            collectionAddress = this.resourceAddress.addressString(),
            localId = Resource.NonFungibleResource.Item.ID.from(id)
        )
    }
    val collection = resources.findNonFungible(resourceAddress.addressString())?.copy(items = items) ?: Resource.NonFungibleResource.from(
        resourceAddress = resourceAddress,
        amount = items.size.toLong(),
        items = items,
        metadata = newlyCreated[resourceAddress.addressString()].orEmpty()
    )

    return TransferableResource.NFTs(
        resource = collection,
        isNewlyCreated = newlyCreated[resourceAddress.addressString()] != null
    )
}

private fun Resource.NonFungibleResource.Companion.from(
    resourceAddress: Address,
    amount: Long,
    items: List<Resource.NonFungibleResource.Item>,
    metadata: Map<String, MetadataValue?>
): Resource.NonFungibleResource = Resource.NonFungibleResource(
    resourceAddress = resourceAddress.addressString(),
    amount = amount,
    nameMetadataItem = metadata[ExplicitMetadataKey.NAME.key]?.let { it as? MetadataValue.StringValue }?.let {
        NameMetadataItem(name = it.value)
    },
    iconMetadataItem = metadata[ExplicitMetadataKey.ICON_URL.key]?.let { it as? MetadataValue.UrlValue }?.let {
        IconUrlMetadataItem(url = Uri.parse(it.value))
    },
    descriptionMetadataItem = metadata[ExplicitMetadataKey.DESCRIPTION.key]?.let { it as? MetadataValue.StringValue }
        ?.let {
            DescriptionMetadataItem(description = it.value)
        },
    tagsMetadataItem = metadata[ExplicitMetadataKey.TAGS.key]?.let { it as MetadataValue.StringArrayValue }?.let {
        TagsMetadataItem(tags = it.value)
    },
    items = items,
)

private val DecimalSource.valueDecimal: BigDecimal
    get() = when (this) {
        is DecimalSource.Guaranteed -> value.asStr().toBigDecimal()
        is DecimalSource.Predicted -> value.asStr().toBigDecimal()
    }

private fun DecimalSource.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType = when (this) {
    is DecimalSource.Guaranteed -> GuaranteeType.Guaranteed
    is DecimalSource.Predicted -> GuaranteeType.Predicted(
        instructionIndex = instructionIndex.toLong(),
        guaranteeOffset = defaultDepositGuarantees
    )
}

private val NonFungibleLocalIdVecSource.valueList: List<NonFungibleLocalId>
    get() = when (this) {
        is NonFungibleLocalIdVecSource.Guaranteed -> value
        is NonFungibleLocalIdVecSource.Predicted -> value
    }

private fun NonFungibleLocalIdVecSource.toGuaranteeType(defaultDepositGuarantees: Double): GuaranteeType = when (this) {
    is NonFungibleLocalIdVecSource.Guaranteed -> GuaranteeType.Guaranteed
    is NonFungibleLocalIdVecSource.Predicted -> GuaranteeType.Predicted(
        instructionIndex = instructionIndex.toLong(),
        guaranteeOffset = defaultDepositGuarantees
    )
}

private fun List<Resource>.findFungible(address: String) = find { it.resourceAddress == address } as? Resource.FungibleResource
private fun List<Resource>.findNonFungible(address: String) = find { it.resourceAddress == address } as? Resource.NonFungibleResource
