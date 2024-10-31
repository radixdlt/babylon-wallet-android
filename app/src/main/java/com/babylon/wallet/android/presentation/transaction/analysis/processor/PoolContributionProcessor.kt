package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.model.TransferableX
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TrackedPoolContribution
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.sumOf
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class PoolContributionProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : PreviewTypeProcessor<DetailedManifestClass.PoolContribution> {

    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolContribution): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(assets)

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transfer.Pool(
            from = withdraws,
            to = deposits.augmentWithContributions(contributions = classification.poolContributions),
            badges = badges,
            actionType = PreviewType.Transfer.Pool.ActionType.Contribution,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }

    private fun List<AccountWithTransferableResources>.augmentWithContributions(
        contributions: List<TrackedPoolContribution>
    ): List<AccountWithTransferableResources> = map { accountWithTransferables ->
        val augmentedTransferables = accountWithTransferables.resources.map { transferable ->
            val poolUnit = (transferable as? TransferableX.FungibleType.PoolUnit) ?: return@map transferable

            var totalPoolUnitAmount = 0.toDecimal192();
            val contributionsPerResource = mutableMapOf<ResourceAddress, Decimal192>()

            contributions
                .filter {
                    it.poolUnitsResourceAddress == poolUnit.resourceAddress
                }.forEach { contribution ->
                    contribution.contributedResources.forEach { (address, amount) ->
                        val currentAmount = contributionsPerResource.getOrDefault(address, 0.toDecimal192())

                        contributionsPerResource[address] = currentAmount + amount
                    }

                    totalPoolUnitAmount += contribution.poolUnitsAmount
                }

            val newAmount = when (poolUnit.amount) {
                is FungibleAmount.Exact -> FungibleAmount.Exact(totalPoolUnitAmount)
                is FungibleAmount.Predicted -> poolUnit.amount.copy(
                    amount = totalPoolUnitAmount
                )
                else -> FungibleAmount.Exact(totalPoolUnitAmount)
            }

            poolUnit.copy(
                amount = newAmount,
                contributionPerResource = contributionsPerResource.mapValues { FungibleAmount.Exact(it.value) }
            )
        }

        accountWithTransferables.update(augmentedTransferables)
    }
}
