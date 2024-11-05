@file:Suppress("TooManyFunctions")

package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FungibleResourceIndicator
import com.radixdlt.sargon.NewlyCreatedResource
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.NonFungibleLocalId
import com.radixdlt.sargon.NonFungibleResourceIndicator
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.amount
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.Resource.NonFungibleResource.Item
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType

fun ExecutionSummary.involvedAddresses(
    excludeNewlyCreated: Boolean = true
): Set<ResourceOrNonFungible> {
    val fungibleResourceAddresses = withdrawals.values.flatten().filterIsInstance<ResourceIndicator.Fungible>() +
        deposits.values.flatten().filterIsInstance<ResourceIndicator.Fungible>()

    val nonFungibleResourceAddresses = withdrawals.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>() +
        deposits.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>()

    val fungibles = fungibleResourceAddresses.asSequence().filterNot {
        excludeNewlyCreated && it.isNewlyCreated(this)
    }.map {
        ResourceOrNonFungible.Resource(it.resourceAddress)
    }.toSet()

    val nonFungibleGlobalIds = nonFungibleResourceAddresses.asSequence().filterNot {
        excludeNewlyCreated && it.isNewlyCreated(this)
    }.map { nonFungible ->
        nonFungible.nonFungibleLocalIds.map { localId ->
            ResourceOrNonFungible.NonFungible(
                NonFungibleGlobalId.init(
                    resourceAddress = nonFungible.resourceAddress,
                    nonFungibleLocalId = localId
                )
            )
        }
    }.flatten().toSet()

    return fungibles + nonFungibleGlobalIds + proofAddresses
}

val ResourceIndicator.amount: Decimal192
    get() = when (this) {
        is ResourceIndicator.Fungible -> {
            when (val specificIndicator = indicator) {
                is FungibleResourceIndicator.Guaranteed -> {
                    specificIndicator.amount
                }

                is FungibleResourceIndicator.Predicted -> specificIndicator.predictedDecimal.value
            }
        }

        is ResourceIndicator.NonFungible -> {
            nonFungibleLocalIds.size.toDecimal192()
        }
    }

fun ResourceIndicator.guaranteeType(defaultGuarantee: Decimal192) = when (this) {
    is ResourceIndicator.Fungible -> when (val indicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> GuaranteeType.Guaranteed
        is FungibleResourceIndicator.Predicted -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedDecimal.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )
    }

    is ResourceIndicator.NonFungible -> when (val indicator = indicator) {
        is NonFungibleResourceIndicator.ByAll -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )

        is NonFungibleResourceIndicator.ByAmount -> GuaranteeType.Predicted(
            instructionIndex = indicator.predictedIds.instructionIndex.toLong(),
            guaranteeOffset = defaultGuarantee
        )

        is NonFungibleResourceIndicator.ByIds -> GuaranteeType.Guaranteed
    }
}

val ResourceOrNonFungible.resourceAddress: ResourceAddress
    get() = when (this) {
        is ResourceOrNonFungible.NonFungible -> value.resourceAddress
        is ResourceOrNonFungible.Resource -> value
    }

fun ResourceIndicator.toTransferableAsset(
    assets: List<Asset>,
    aggregateAmount: Decimal192? = null
): TransferableAsset = when (this) {
    is ResourceIndicator.Fungible -> toTransferableAsset(assets, aggregateAmount)
    is ResourceIndicator.NonFungible -> toTransferableAsset(assets)
}

@Suppress("CyclomaticComplexMethod")
private fun ResourceIndicator.Fungible.toTransferableAsset(
    assets: List<Asset>,
    aggregateAmount: Decimal192? = null
): TransferableAsset.Fungible = when (val asset = assets.find { it.resource.address == resourceAddress }) {
    is PoolUnit -> {
        val assetWithAmount = asset.copy(
            stake = asset.stake.copy(ownedAmount = aggregateAmount ?: amount),
            pool = asset.pool
        )
        TransferableAsset.Fungible.PoolUnitAsset(
            amount = aggregateAmount ?: amount,
            unit = assetWithAmount,
            contributionPerResource = assetWithAmount.pool?.resources?.associate {
                it.address to (assetWithAmount.resourceRedemptionValue(it).orZero())
            }.orEmpty(),
            isNewlyCreated = false
        )
    }

    is LiquidStakeUnit -> {
        val assetWithAmount = asset.copy(fungibleResource = asset.fungibleResource.copy(ownedAmount = aggregateAmount ?: amount))
        TransferableAsset.Fungible.LSUAsset(
            amount = aggregateAmount ?: amount,
            lsu = assetWithAmount,
            xrdWorth = assetWithAmount.stakeValueInXRD(asset.validator.totalXrdStake).orZero(),
            isNewlyCreated = false
        )
    }

    is Token -> {
        val resourceWithAmount = asset.resource.copy(ownedAmount = aggregateAmount ?: amount)
        TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = resourceWithAmount,
            isNewlyCreated = false
        )
    }

    else -> {
        val resourceWithAmount = Resource.FungibleResource(
            address = resourceAddress,
            ownedAmount = aggregateAmount ?: amount
        )
        TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = resourceWithAmount,
            isNewlyCreated = false
        )
    }
}

private fun ResourceIndicator.NonFungible.toTransferableAsset(
    assets: List<Asset>
): TransferableAsset.NonFungible = when (val asset = assets.find { it.resource.address == resourceAddress }) {
    is StakeClaim -> {
        val items = nonFungibleLocalIds.map { localId ->
            asset.nonFungibleResource.items.find { item ->
                item.localId == localId
            } ?: Item(
                collectionAddress = resourceAddress,
                localId = localId
            )
        }
        val assetWithItems = asset.copy(nonFungibleResource = asset.nonFungibleResource.copy(items = items))

        TransferableAsset.NonFungible.StakeClaimAssets(
            claim = assetWithItems,
            xrdWorthPerNftItem = items.associate { it.localId to it.claimAmountXrd.orZero() },
            isNewlyCreated = false
        )
    }

    is NonFungibleCollection -> {
        val items = nonFungibleLocalIds.map { localId ->
            asset.collection.items.find { item ->
                item.localId == localId
            } ?: Item(
                collectionAddress = resourceAddress,
                localId = localId
            )
        }

        TransferableAsset.NonFungible.NFTAssets(
            resource = asset.collection.copy(items = items),
            isNewlyCreated = false
        )
    }

    else -> {
        val items = nonFungibleLocalIds.map { localId ->
            Item(
                collectionAddress = resourceAddress,
                localId = localId
            )
        }
        val amount = items.size.toLong()

        TransferableAsset.NonFungible.NFTAssets(
            resource = Resource.NonFungibleResource(
                address = resourceAddress,
                amount = amount,
                items = items
            ),
            isNewlyCreated = false
        )
    }
}

fun ResourceIndicator.isNewlyCreated(summary: ExecutionSummary) = summary.newEntities.metadata.containsKey(address)

fun ResourceIndicator.newlyCreatedResource(summary: ExecutionSummary) = summary.newEntities.metadata[address]

fun ResourceIndicator.toNewlyCreatedTransferableAsset(
    resource: NewlyCreatedResource,
    aggregateAmount: Decimal192? = null
): TransferableAsset {
    val metadataItems = resource.toMetadata()

    return when (this) {
        is ResourceIndicator.Fungible -> TransferableAsset.Fungible.Token(
            amount = aggregateAmount ?: amount,
            resource = Resource.FungibleResource(
                address = resourceAddress,
                ownedAmount = aggregateAmount ?: amount,
                metadata = metadataItems
            ),
            isNewlyCreated = true
        )

        is ResourceIndicator.NonFungible -> {
            val items = nonFungibleLocalIds.map { localId ->
                Item(
                    collectionAddress = resourceAddress,
                    localId = localId
                )
            }
            val amount = items.size.toLong()

            TransferableAsset.NonFungible.NFTAssets(
                resource = Resource.NonFungibleResource(
                    address = resourceAddress,
                    amount = amount,
                    items = items,
                    metadata = metadataItems
                ),
                isNewlyCreated = true
            )
        }
    }
}

private val ResourceIndicator.Fungible.amount: Decimal192
    get() = when (val specificIndicator = indicator) {
        is FungibleResourceIndicator.Guaranteed -> specificIndicator.amount
        is FungibleResourceIndicator.Predicted -> specificIndicator.predictedDecimal.value
    }

val ResourceIndicator.NonFungible.nonFungibleLocalIds: List<NonFungibleLocalId>
    get() = when (val indicator = indicator) {
        is NonFungibleResourceIndicator.ByAll -> indicator.predictedIds.value
        is NonFungibleResourceIndicator.ByAmount -> indicator.predictedIds.value
        is NonFungibleResourceIndicator.ByIds -> indicator.ids
    }

fun List<Transferable>.toAccountWithTransferableResources(
    accountAddress: AccountAddress,
    ownedAccounts: List<Account>
): AccountWithTransferableResources {
    val ownedAccount = ownedAccounts.find { it.address == accountAddress }
    return if (ownedAccount != null) {
        AccountWithTransferableResources.Owned(ownedAccount, this)
    } else {
        AccountWithTransferableResources.Other(accountAddress, this)
    }
}

fun ExecutionSummary.involvedOwnedAccounts(ownedAccounts: List<Account>): List<Account> {
    val involvedAccountAddresses = withdrawals.keys + deposits.keys
    return ownedAccounts.filter {
        it.address in involvedAccountAddresses
    }
}

fun ExecutionSummary.toWithdrawingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Account>,
    aggregateByResourceAddress: Boolean = false // for now I turned aggregation off to not introduce any bugs
): List<AccountWithTransferableResources> {
    return withdrawals.map { withdrawEntry ->
        if (aggregateByResourceAddress) {
            withdrawEntry.value.groupBy { it.address }.map { indicatorsPerResource ->
                val resources = indicatorsPerResource.value
                when (val first = resources.first()) {
                    is ResourceIndicator.Fungible -> {
                        val aggregateAmount = resources.map { it.amount }.sumOf { it }

                        first.newlyCreatedResource(summary = this)?.let { newlyCreatedResource ->
                            first.toNewlyCreatedTransferableAsset(newlyCreatedResource, aggregateAmount)
                        } ?: first.toTransferableAsset(involvedAssets, aggregateAmount)
                    }

                    is ResourceIndicator.NonFungible -> {
                        val nonFungibleLocalIds = resources.filterIsInstance<ResourceIndicator.NonFungible>()
                            .fold(setOf<NonFungibleLocalId>(), operation = { acc, resource ->
                                acc + resource.nonFungibleLocalIds.toSet()
                            })
                        val resource = first.copy(indicator = NonFungibleResourceIndicator.ByIds(nonFungibleLocalIds.toList()))
                        resource.newlyCreatedResource(summary = this)?.let { newlyCreatedResource ->
                            resource.toNewlyCreatedTransferableAsset(newlyCreatedResource)
                        } ?: resource.toTransferableAsset(involvedAssets)
                    }
                }
            }
        } else {
            withdrawEntry.value.map { resource ->
                resource.newlyCreatedResource(summary = this)?.let { newlyCreatedResource ->
                    resource.toNewlyCreatedTransferableAsset(newlyCreatedResource)
                } ?: resource.toTransferableAsset(involvedAssets)
            }
        }.map {
            Transferable.Withdrawing(it)
        }.toAccountWithTransferableResources(
            withdrawEntry.key,
            allOwnedAccounts
        )
    }.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))
}

fun ExecutionSummary.resolveDepositingAsset(
    resourceIndicator: ResourceIndicator,
    involvedAssets: List<Asset>,
    defaultDepositGuarantee: Decimal192,
    aggregateAmount: Decimal192? = null
): Transferable.Depositing {
    val asset = resourceIndicator.newlyCreatedResource(summary = this)?.let { newlyCreatedResource ->
        resourceIndicator.toNewlyCreatedTransferableAsset(newlyCreatedResource)
    } ?: resourceIndicator.toTransferableAsset(involvedAssets, aggregateAmount)

    return Transferable.Depositing(
        transferable = asset,
        guaranteeType = resourceIndicator.guaranteeType(defaultDepositGuarantee)
    )
}

fun ExecutionSummary.toDepositingAccountsWithTransferableAssets(
    involvedAssets: List<Asset>,
    allOwnedAccounts: List<Account>,
    defaultGuarantee: Decimal192
) = deposits.map { depositEntry ->
    depositEntry.value.map { resource ->
        resolveDepositingAsset(resource, involvedAssets, defaultGuarantee)
    }.toAccountWithTransferableResources(
        depositEntry.key,
        allOwnedAccounts
    )
}.sortedWith(AccountWithTransferableResources.Companion.Sorter(allOwnedAccounts))

val ExecutionSummary.proofAddresses: List<ResourceOrNonFungible>
    get() = presentedProofs.map { specifier ->
        when (specifier) {
            is ResourceSpecifier.Fungible -> listOf(ResourceOrNonFungible.Resource(specifier.resourceAddress))
            is ResourceSpecifier.NonFungible -> specifier.ids.map { localId ->
                ResourceOrNonFungible.NonFungible(
                    NonFungibleGlobalId.init(
                        resourceAddress = specifier.resourceAddress,
                        nonFungibleLocalId = localId
                    )
                )
            }
        }
    }.flatten()

fun ExecutionSummary.resolveBadges(assets: List<Asset>): List<Badge> {
    val proofAddresses = presentedProofs.map { it.address }

    return assets.filter { asset ->
        asset.resource.address in proofAddresses
    }.mapNotNull { asset ->
        val specifier = presentedProofs.find { it.address == asset.resource.address } ?: return@mapNotNull null

        val badgeResource = when (specifier) {
            is ResourceSpecifier.Fungible -> {
                // In this case we need to attach the amount of the specifier to the resource since it is not resolved by GW
                (asset.resource as? Resource.FungibleResource)?.copy(ownedAmount = specifier.amount) ?: return@mapNotNull null
            }

            is ResourceSpecifier.NonFungible -> asset.resource
        }

        Badge(resource = badgeResource)
    }
}

fun ExecutionSummary.newlyCreatedNonFungibleItems() = newlyCreatedNonFungibles.map {
    Item(
        it.resourceAddress,
        it.nonFungibleLocalId
    )
}

private fun NewlyCreatedResource.toMetadata(): List<Metadata> {
    val metadata = mutableListOf<Metadata>()

    name?.let {
        metadata.add(Metadata.Primitive(key = ExplicitMetadataKey.NAME.key, value = it, valueType = MetadataType.String))
    }

    symbol?.let {
        metadata.add(Metadata.Primitive(key = ExplicitMetadataKey.SYMBOL.key, value = it, valueType = MetadataType.String))
    }

    description?.let {
        metadata.add(Metadata.Primitive(key = ExplicitMetadataKey.DESCRIPTION.key, value = it, valueType = MetadataType.String))
    }

    iconUrl?.let {
        metadata.add(Metadata.Primitive(key = ExplicitMetadataKey.ICON_URL.key, value = it, valueType = MetadataType.Url))
    }

    tags.takeIf { it.isNotEmpty() }?.let {
        metadata.add(
            Metadata.Collection(
                key = ExplicitMetadataKey.TAGS.key,
                values = it.map { tag ->
                    Metadata.Primitive(key = ExplicitMetadataKey.TAGS.key, value = tag, valueType = MetadataType.String)
                }
            )
        )
    }

    return metadata
}
