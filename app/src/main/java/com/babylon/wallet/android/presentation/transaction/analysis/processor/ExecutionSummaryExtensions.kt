package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.RadixWalletException.ResourceCouldNotBeResolvedInTransaction
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.FungibleResourceIndicator
import com.radixdlt.sargon.NewlyCreatedResource
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.extensions.Accounts
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.asIdentifiable
import com.radixdlt.sargon.extensions.ids
import com.radixdlt.sargon.extensions.orZero
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
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork

/**
 * Resolves all the addresses involved in the transaction and can be queried from gateway. That means that newly created
 * fungibles and NFTs must be excluded.
 *
 * @return The [ResourceOrNonFungible]s to query on gateway.
 */
fun ExecutionSummary.involvedAddresses(): Set<ResourceOrNonFungible> {
    val fungibleIndicators = withdrawals.values.flatten().filterIsInstance<ResourceIndicator.Fungible>() +
            deposits.values.flatten().filterIsInstance<ResourceIndicator.Fungible>()

    val nonFungibleIndicators = withdrawals.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>() +
            deposits.values.flatten().filterIsInstance<ResourceIndicator.NonFungible>()

    val fungibles = fungibleIndicators.asSequence().filterNot {
        newEntities.metadata.containsKey(it.address)
    }.map {
        ResourceOrNonFungible.Resource(it.resourceAddress)
    }.toSet()

    val nonFungibleGlobalIds = nonFungibleIndicators.asSequence().filterNot {
        newEntities.metadata.containsKey(it.address)
    }.map { nonFungibleIndicator ->
        nonFungibleIndicator.indicator.ids.mapNotNull { localId ->
            val globalId = NonFungibleGlobalId(
                resourceAddress = nonFungibleIndicator.resourceAddress,
                nonFungibleLocalId = localId
            )

            if (globalId !in newlyCreatedNonFungibles) {
                ResourceOrNonFungible.NonFungible(globalId)
            } else {
                null
            }
        }
    }.flatten().toSet()

    return fungibles + nonFungibleGlobalIds + involvedProofAddresses()
}

/**
 * Extracts the [Badge]s involved in the transaction.
 *
 * @return A [Badge] that can be Fungible or NonFungible.
 */
fun ExecutionSummary.resolveBadges(onLedgerAssets: List<Asset>): List<Badge> {
    val proofAddresses = presentedProofs.associateBy { it.address }

    return onLedgerAssets.filter { asset ->
        asset.resource.address in proofAddresses.keys
    }.mapNotNull { asset ->
        val specifier = proofAddresses[asset.resource.address] ?: return@mapNotNull null

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

/**
 * Extracts the addresses involved in presenting proofs
 *
 * @return The [ResourceOrNonFungible] addresses involved as proofs.
 */
fun ExecutionSummary.involvedProofAddresses(): List<ResourceOrNonFungible> = presentedProofs.map { specifier ->
    when (specifier) {
        is ResourceSpecifier.Fungible -> listOf(ResourceOrNonFungible.Resource(specifier.resourceAddress))
        is ResourceSpecifier.NonFungible -> specifier.ids.map { localId ->
            ResourceOrNonFungible.NonFungible(
                NonFungibleGlobalId(
                    resourceAddress = specifier.resourceAddress,
                    nonFungibleLocalId = localId
                )
            )
        }
    }
}.flatten()

/**
 * Resolves all newly created NFTs as [Resource.NonFungibleResource.Item]s.
 *
 * @return A list of [Resource.NonFungibleResource.Item] with empty data.
 */
fun ExecutionSummary.resolveNewlyCreatedNFTs() = newlyCreatedNonFungibles.map {
    Item(
        it.resourceAddress,
        it.nonFungibleLocalId
    )
}

/**
 * Extracts information for all accounts and the transferables being involved in the transaction.
 *
 * @return A pair of withdraws and deposits.
 */
fun ExecutionSummary.resolveWithdrawsAndDeposits(
    onLedgerAssets: List<Asset>,
    profile: Profile
): Pair<List<AccountWithTransferables>, List<AccountWithTransferables>> {
    val involvedAccounts = involvedProfileAccounts(profile)
    val defaultDepositGuarantee = profile.appPreferences.transaction.defaultDepositGuarantee;

    val withdrawsPerAccount = resolveAccounts(
        profileAccounts = involvedAccounts,
        resourceIndicators = withdrawals,
        onLedgerAssets = onLedgerAssets,
        defaultDepositGuarantee = 0.toDecimal192()
    ).sortedWith(AccountWithTransferables.Companion.Sorter(profile))

    val depositsPerAccount = resolveAccounts(
        profileAccounts = involvedAccounts,
        resourceIndicators = deposits,
        onLedgerAssets = onLedgerAssets,
        defaultDepositGuarantee = defaultDepositGuarantee
    ).sortedWith(AccountWithTransferables.Companion.Sorter(profile))

    return withdrawsPerAccount to depositsPerAccount
}

private fun ExecutionSummary.involvedProfileAccounts(profile: Profile): Accounts {
    val involvedAccountAddresses = (withdrawals.keys + deposits.keys)

    val accountsToSearch = profile.activeAccountsOnCurrentNetwork.asIdentifiable();
    return involvedAccountAddresses.mapNotNull { address ->
        accountsToSearch.getBy(address)
    }.asIdentifiable()
}

private fun NewlyCreatedResource.toMetadata(): List<Metadata> {
    val metadata = mutableListOf<Metadata>()

    name?.let {
        metadata.add(
            Metadata.Primitive(
                key = ExplicitMetadataKey.NAME.key,
                value = it,
                valueType = MetadataType.String
            )
        )
    }

    symbol?.let {
        metadata.add(
            Metadata.Primitive(
                key = ExplicitMetadataKey.SYMBOL.key,
                value = it,
                valueType = MetadataType.String
            )
        )
    }

    description?.let {
        metadata.add(
            Metadata.Primitive(
                key = ExplicitMetadataKey.DESCRIPTION.key,
                value = it,
                valueType = MetadataType.String
            )
        )
    }

    iconUrl?.let {
        metadata.add(
            Metadata.Primitive(
                key = ExplicitMetadataKey.ICON_URL.key,
                value = it,
                valueType = MetadataType.Url
            )
        )
    }

    val tags = tags.map {
        Metadata.Primitive(
            key = ExplicitMetadataKey.TAGS.key,
            value = it,
            valueType = MetadataType.String
        )
    }
    if (tags.isNotEmpty()) {
        metadata.add(
            Metadata.Collection(
                key = ExplicitMetadataKey.TAGS.key,
                values = tags
            )
        )
    }

    return metadata
}

private fun ResourceIndicator.Fungible.amount(defaultGuaranteeOffset: Decimal192) = when (val fungibleIndicator = indicator) {
    is FungibleResourceIndicator.Guaranteed -> FungibleAmount.Exact(fungibleIndicator.decimal)
    is FungibleResourceIndicator.Predicted -> FungibleAmount.Predicted(
        estimated = fungibleIndicator.predictedDecimal.value,
        instructionIndex = fungibleIndicator.predictedDecimal.instructionIndex.toLong(),
        percent = defaultGuaranteeOffset
    )
}

private fun ResourceIndicator.NonFungible.amount(asset: Asset.NonFungible): NonFungibleAmount {
    val onLedgerItems = asset.resource.items

    val items = indicator.ids.map { localId ->
        onLedgerItems.find { it.localId == localId } ?: Resource.NonFungibleResource.Item(
            collectionAddress = asset.resource.address, localId = localId
        )
    }

    return NonFungibleAmount.Exact(nfts = items)
}

private fun ExecutionSummary.resolveAsset(
    resourceIndicator: ResourceIndicator,
    onLedgerAssets: List<Asset>
): Pair<Asset, Boolean> = when (resourceIndicator) {
    is ResourceIndicator.Fungible -> {
        val newEntityMetadata = newEntities.metadata[resourceIndicator.address]
        if (newEntityMetadata != null) {
            Token(
                resource = Resource.FungibleResource(
                    address = resourceIndicator.resourceAddress,
                    ownedAmount = 0.toDecimal192(), // This amount is irrelevant
                    metadata = newEntityMetadata.toMetadata()
                )
            ) to true
        } else {
            val asset = onLedgerAssets.find {
                it.resource.address == resourceIndicator.address
            } as? Asset.Fungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
            )

            asset to false
        }
    }

    is ResourceIndicator.NonFungible -> {
        val ids = resourceIndicator.indicator.ids

        val newEntityMetadata = newEntities.metadata[resourceIndicator.address]
        if (newEntityMetadata != null) {
            NonFungibleCollection(
                collection = Resource.NonFungibleResource(
                    address = resourceIndicator.resourceAddress,
                    amount = 0, // This amount is irrelevant
                    metadata = newEntityMetadata.toMetadata(),
                    items = ids.map {
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceIndicator.resourceAddress,
                            localId = it
                        )
                    }
                )
            ) to true
        } else {
            val nonFungibleAsset = onLedgerAssets.find {
                it.resource.address == resourceIndicator.address
            } as? Asset.NonFungible ?: throw ResourceCouldNotBeResolvedInTransaction(
                ResourceOrNonFungible.Resource(resourceIndicator.resourceAddress)
            )

            val onLedgerNFTs = nonFungibleAsset.resource.items.associateBy { it.localId }
            val newlyCreatedNFTs = newlyCreatedNonFungibles.mapNotNull {
                if (it.resourceAddress == resourceIndicator.resourceAddress) {
                    it.nonFungibleLocalId
                } else {
                    null
                }
            }

            val items = ids.map { id ->
                if (id in newlyCreatedNFTs) {
                    Resource.NonFungibleResource.Item(
                        collectionAddress = resourceIndicator.resourceAddress,
                        localId = id
                    )
                } else {
                    onLedgerNFTs[id] ?: throw ResourceCouldNotBeResolvedInTransaction(
                        ResourceOrNonFungible.NonFungible(
                            NonFungibleGlobalId(
                                resourceAddress = resourceIndicator.resourceAddress,
                                nonFungibleLocalId = id
                            )
                        )
                    )
                }
            }

            when (nonFungibleAsset) {
                is NonFungibleCollection -> nonFungibleAsset.copy(
                    collection = nonFungibleAsset.collection.copy(items = items)
                )

                is StakeClaim -> nonFungibleAsset.copy(
                    nonFungibleResource = nonFungibleAsset.nonFungibleResource.copy(items = items)
                )
            } to false
        }
    }
}

private fun ExecutionSummary.resolveTransferable(
    resourceIndicator: ResourceIndicator,
    onLedgerAssets: List<Asset>,
    defaultDepositGuarantee: Decimal192
): Transferable {
    val (asset, isNewlyCreated) = resolveAsset(
        resourceIndicator = resourceIndicator,
        onLedgerAssets = onLedgerAssets
    )

    return when (asset) {
        is Token -> Transferable.FungibleType.Token(
            asset = asset,
            amount = (resourceIndicator as ResourceIndicator.Fungible).amount(defaultDepositGuarantee),
            isNewlyCreated = isNewlyCreated
        )

        is LiquidStakeUnit -> {
            val amount = (resourceIndicator as ResourceIndicator.Fungible).amount(defaultDepositGuarantee)
            val amountDecimal = when (amount) {
                is FungibleAmount.Exact -> amount.amount
                is FungibleAmount.Predicted -> amount.estimated
                else -> TODO()
            }

            Transferable.FungibleType.LSU(
                asset = asset,
                amount = amount,
                xrdWorth = asset.stakeValueXRD(ownedAmount = amountDecimal).orZero(),
                isNewlyCreated = isNewlyCreated
            )
        }

        is PoolUnit -> {
            val amount = (resourceIndicator as ResourceIndicator.Fungible).amount(defaultDepositGuarantee)
            Transferable.FungibleType.PoolUnit(
                asset = asset,
                amount = amount,
                isNewlyCreated = isNewlyCreated,
                contributionPerResource = asset.pool?.resources?.mapNotNull { entry ->
                    val contribution = when (amount) {
                        is FungibleAmount.Exact -> asset.poolItemRedemptionValue(
                            address = entry.address,
                            poolUnitAmount = amount.amount
                        )?.let { FungibleAmount.Exact(amount = it) }

                        is FungibleAmount.Predicted -> asset.poolItemRedemptionValue(
                            address = entry.address,
                            poolUnitAmount = amount.estimated
                        )?.let { FungibleAmount.Exact(amount = it) }
                        is FungibleAmount.Max -> asset.poolItemRedemptionValue(
                            address = entry.address,
                            poolUnitAmount = amount.amount
                        )?.let { FungibleAmount.Max(amount = it) }
                        is FungibleAmount.Min -> asset.poolItemRedemptionValue(
                            address = entry.address,
                            poolUnitAmount = amount.amount
                        )?.let { FungibleAmount.Min(amount = it) }
                        is FungibleAmount.Range -> {
                            val min = asset.poolItemRedemptionValue(
                                address = entry.address,
                                poolUnitAmount = amount.minAmount
                            )

                            val max = asset.poolItemRedemptionValue(
                                address = entry.address,
                                poolUnitAmount = amount.maxAmount
                            )

                            if (min != null && max != null) {
                                FungibleAmount.Range(
                                    minAmount = min,
                                    maxAmount = max
                                )
                            } else {
                                null
                            }
                        }
                        is FungibleAmount.Unknown -> null
                    }

                    if (contribution == null) return@mapNotNull null

                    entry.address to contribution
                }?.associate { it }.orEmpty()
            )
        }

        is NonFungibleCollection -> Transferable.NonFungibleType.NFTCollection(
            asset = asset,
            amount = (resourceIndicator as ResourceIndicator.NonFungible).amount(asset),
            isNewlyCreated = isNewlyCreated,
        )

        is StakeClaim -> Transferable.NonFungibleType.StakeClaim(
            asset = asset,
            amount = (resourceIndicator as ResourceIndicator.NonFungible).amount(asset),
            xrdWorthPerNftItem = asset.nonFungibleResource.items.associate { it.localId to it.claimAmountXrd.orZero() },
            isNewlyCreated = isNewlyCreated
        )
    }
}

private fun ExecutionSummary.resolveAccounts(
    profileAccounts: Accounts,
    resourceIndicators: Map<AccountAddress, List<ResourceIndicator>>,
    onLedgerAssets: List<Asset>,
    defaultDepositGuarantee: Decimal192
) = resourceIndicators.map { entry ->
    val transferables = entry.value.map { indicator ->
        resolveTransferable(
            resourceIndicator = indicator,
            onLedgerAssets = onLedgerAssets,
            defaultDepositGuarantee = defaultDepositGuarantee
        )
    }

    val profileAccount = profileAccounts.getBy(entry.key)
    if (profileAccount != null) {
        AccountWithTransferables.Owned(
            account = profileAccount,
            transferables = transferables
        )
    } else {
        AccountWithTransferables.Other(
            address = entry.key,
            transferables = transferables
        )
    }
}
