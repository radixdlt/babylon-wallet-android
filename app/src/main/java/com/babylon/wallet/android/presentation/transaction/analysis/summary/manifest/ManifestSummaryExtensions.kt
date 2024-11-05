package com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest

import com.babylon.wallet.android.domain.RadixWalletException.ResourceCouldNotBeResolvedInTransaction
import com.babylon.wallet.android.presentation.model.CountedAmount
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.InvolvedAccount
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.AccountWithdraw
import com.radixdlt.sargon.ManifestSummary
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.Profile
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.ResourceSpecifier
import com.radixdlt.sargon.SimpleCountedResourceBounds
import com.radixdlt.sargon.SimpleResourceBounds
import com.radixdlt.sargon.UnspecifiedResources
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.orZero
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.NonFungibleCollection
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.assets.Token
import rdx.works.core.domain.resources.Badge
import rdx.works.core.domain.resources.Resource
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.toResourceOrNonFungible

/**
 * Extracts a set of resource addresses or global ids that are involved in this transaction. It includes resources
 * in withdraws, deposits and the presented proofs.
 *
 * @return [ResourceOrNonFungible] addresses to resolve to the gateway.
 */
fun ManifestSummary.involvedResourceAddresses(): Set<ResourceOrNonFungible> {
    val withdrawAddresses = accountWithdrawals.values.flatten().map { withdraw ->
        when (withdraw) {
            is AccountWithdraw.Amount -> listOf(ResourceOrNonFungible.Resource(withdraw.resourceAddress))
            is AccountWithdraw.Ids -> withdraw.ids.map { id ->
                ResourceOrNonFungible.NonFungible(
                    NonFungibleGlobalId(
                        resourceAddress = withdraw.resourceAddress,
                        nonFungibleLocalId = id
                    )
                )
            }
        }
    }.flatten().toSet()

    val depositAddresses = accountDeposits.values.map { deposit ->
        deposit.specifiedResources.map { bounds ->
            when (bounds) {
                is SimpleResourceBounds.Fungible -> listOf(ResourceOrNonFungible.Resource(bounds.resourceAddress))
                is SimpleResourceBounds.NonFungible -> {
                    bounds.bounds.certainIds.map { id ->
                        ResourceOrNonFungible.NonFungible(
                            value = NonFungibleGlobalId(
                                resourceAddress = bounds.resourceAddress,
                                nonFungibleLocalId = id
                            )
                        )
                    }
                }
            }
        }.flatten()
    }.flatten().toSet()

    val proofAddresses = presentedProofs.map { specifier ->
        specifier.toResourceOrNonFungible()
    }.flatten().toSet()

    return withdrawAddresses + depositAddresses + proofAddresses
}

/**
 * Matches the involved resources to assets.
 *
 * @return A pair of [AccountWithTransferables] lists for both withdraws and deposits.
 */
fun ManifestSummary.resolveWithdrawsAndDeposits(
    onLedgerAssets: List<Asset>,
    profile: Profile
): Pair<List<AccountWithTransferables>, List<AccountWithTransferables>> = resolveWithdraws(
    onLedgerAssets = onLedgerAssets,
    profile = profile
) to resolveDeposits(
    onLedgerAssets = onLedgerAssets,
    profile = profile
)

/**
 * Extracts the [Badge]s involved in the transaction.
 *
 * @return A [Badge] that can be Fungible or NonFungible.
 */
fun ManifestSummary.resolveBadges(onLedgerAssets: List<Asset>): List<Badge> {
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

private fun AccountAddress.toInvolvedAccount(profile: Profile): InvolvedAccount {
    val profileAccount = profile.activeAccountsOnCurrentNetwork.find { it.address == this }

    return if (profileAccount != null) {
        InvolvedAccount.Owned(profileAccount)
    } else {
        InvolvedAccount.Other(this)
    }
}

private fun ManifestSummary.resolveWithdraws(
    onLedgerAssets: List<Asset>,
    profile: Profile
): List<AccountWithTransferables> = accountWithdrawals.map { entry ->
    val transferables = entry.value.map { withdraw ->
        when (withdraw) {
            is AccountWithdraw.Amount -> {
                val asset = onLedgerAssets.find { it.resource.address == withdraw.resourceAddress } as? Asset.Fungible
                    ?: throw ResourceCouldNotBeResolvedInTransaction(withdraw.resourceAddress)
                val amount = CountedAmount.Exact(withdraw.amount)

                when (asset) {
                    is Token -> Transferable.FungibleType.Token(
                        asset = asset,
                        amount = amount
                    )

                    is LiquidStakeUnit -> Transferable.FungibleType.LSU(
                        asset = asset,
                        amount = amount,
                        xrdWorth = amount.calculateWith { decimal -> asset.stakeValueXRD(lsu = decimal).orZero() }
                    )

                    is PoolUnit -> Transferable.FungibleType.PoolUnit(
                        asset = asset,
                        amount = amount
                    )
                }
            }

            is AccountWithdraw.Ids -> {
                val asset = onLedgerAssets.find { it.resource.address == withdraw.resourceAddress } as? Asset.NonFungible
                    ?: throw ResourceCouldNotBeResolvedInTransaction(withdraw.resourceAddress)

                val items = withdraw.ids.map { id ->
                    asset.resource.items.find {
                        it.localId == id
                    } ?: throw ResourceCouldNotBeResolvedInTransaction(
                        resourceAddress = withdraw.resourceAddress,
                        localId = id
                    )
                }

                when (asset) {
                    is NonFungibleCollection -> Transferable.NonFungibleType.NFTCollection(
                        asset = asset,
                        amount = NonFungibleAmount(items)
                    )

                    is StakeClaim -> Transferable.NonFungibleType.StakeClaim(
                        asset = asset,
                        amount = NonFungibleAmount(items)
                    )
                }
            }
        }
    }

    AccountWithTransferables(
        account = entry.key.toInvolvedAccount(profile),
        transferables = transferables
    )
}

private fun ManifestSummary.resolveDeposits(
    onLedgerAssets: List<Asset>,
    profile: Profile
): List<AccountWithTransferables> = accountDeposits.map { entry ->
    val specifiedTransferables = entry.value.specifiedResources.map { specifier ->
        when (specifier) {
            is SimpleResourceBounds.Fungible -> {
                val asset = onLedgerAssets.find { it.resource.address == specifier.resourceAddress } as? Asset.Fungible
                    ?: throw ResourceCouldNotBeResolvedInTransaction(specifier.resourceAddress)

                val amount = specifier.resolveAmount()

                when (asset) {
                    is Token -> Transferable.FungibleType.Token(
                        asset = asset,
                        amount = amount,
                    )

                    is LiquidStakeUnit -> Transferable.FungibleType.LSU(
                        asset = asset,
                        amount = amount,
                        xrdWorth = amount.calculateWith { decimal ->
                            asset.stakeValueXRD(lsu = decimal).orZero()
                        }
                    )

                    is PoolUnit -> Transferable.FungibleType.PoolUnit(
                        asset = asset,
                        amount = amount
                    )
                }
            }

            is SimpleResourceBounds.NonFungible -> {
                val asset = onLedgerAssets.find { it.resource.address == specifier.resourceAddress } as? Asset.NonFungible
                    ?: throw ResourceCouldNotBeResolvedInTransaction(specifier.resourceAddress)

                val certainItems = specifier.bounds.certainIds.map { id ->
                    asset.resource.items.find { it.localId == id } ?: throw ResourceCouldNotBeResolvedInTransaction(
                        specifier.resourceAddress,
                        id
                    )
                }

                val amount = when (val additionalAmount = specifier.resolveAmount()) {
                    null -> NonFungibleAmount(certainItems)
                    else -> NonFungibleAmount(
                        certain = certainItems,
                        additional = additionalAmount
                    )
                }

                when (asset) {
                    is NonFungibleCollection -> Transferable.NonFungibleType.NFTCollection(
                        asset = asset,
                        amount = amount
                    )

                    is StakeClaim -> Transferable.NonFungibleType.StakeClaim(
                        asset = asset,
                        amount = amount
                    )
                }
            }
        }
    }

    AccountWithTransferables(
        account = entry.key.toInvolvedAccount(profile),
        transferables = specifiedTransferables,
        additionalTransferablesPresent = entry.value.unspecifiedResources == UnspecifiedResources.MAY_BE_PRESENT
    )
}

private fun SimpleResourceBounds.Fungible.resolveAmount() = when (val bounds = bounds) {
    is SimpleCountedResourceBounds.AtLeast -> CountedAmount.Min(bounds.amount)
    is SimpleCountedResourceBounds.AtMost -> CountedAmount.Max(bounds.amount)
    is SimpleCountedResourceBounds.Between -> CountedAmount.Range(
        minAmount = bounds.minAmount,
        maxAmount = bounds.maxAmount
    )

    is SimpleCountedResourceBounds.Exact -> CountedAmount.Exact(bounds.amount)
    is SimpleCountedResourceBounds.UnknownAmount -> CountedAmount.Unknown
}

private fun SimpleResourceBounds.NonFungible.resolveAmount() = when (val bounds = bounds.additionalAmount) {
    is SimpleCountedResourceBounds.AtLeast -> CountedAmount.Min(bounds.amount)
    is SimpleCountedResourceBounds.AtMost -> CountedAmount.Max(bounds.amount)
    is SimpleCountedResourceBounds.Between -> CountedAmount.Range(
        minAmount = bounds.minAmount,
        maxAmount = bounds.maxAmount
    )

    is SimpleCountedResourceBounds.Exact -> CountedAmount.Exact(
        amount = bounds.amount
    )

    is SimpleCountedResourceBounds.UnknownAmount -> CountedAmount.Unknown
    null -> null
}
