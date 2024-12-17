package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TrackedPoolContribution
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
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

        val augmentedDeposits = deposits.augmentWithContributions(contributions = classification.poolContributions)

        val involvedPools = (withdraws + augmentedDeposits).toSet().map { accountWithTransferables ->
            accountWithTransferables.transferables.mapNotNull {
                (it as? Transferable.FungibleType.PoolUnit)?.asset?.pool
            }
        }.flatten().toSet()

        return PreviewType.Transaction(
            from = withdraws,
            to = augmentedDeposits,
            badges = badges,
            involvedComponents = PreviewType.Transaction.InvolvedComponents.Pools(
                pools = involvedPools,
                actionType = PreviewType.Transaction.InvolvedComponents.Pools.ActionType.Contribution
            ),
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }

    private fun List<AccountWithTransferables>.augmentWithContributions(
        contributions: List<TrackedPoolContribution>
    ): List<AccountWithTransferables> = map { accountWithTransferables ->
        val augmentedTransferables = accountWithTransferables.transferables.map tr@{ transferable ->
            val poolUnit = (transferable as? Transferable.FungibleType.PoolUnit) ?: return@tr transferable

            var totalPoolUnitAmount = poolUnit.amount.just(0.toDecimal192())
            val contributionsPerResource = mutableMapOf<ResourceAddress, BoundedAmount>()

            contributions.filter {
                it.poolUnitsResourceAddress == poolUnit.resourceAddress
            }.forEach { contribution ->
                contribution.contributedResources.forEach { (address, amount) ->
                    val currentAmount = contributionsPerResource.getOrDefault(address, poolUnit.amount.just(0.toDecimal192()))

                    contributionsPerResource[address] = currentAmount.calculateWith { it + amount }
                }

                totalPoolUnitAmount = totalPoolUnitAmount.calculateWith { it + contribution.poolUnitsAmount }
            }

            poolUnit.copy(
                amount = totalPoolUnitAmount,
                contributions = contributionsPerResource
            )
        }

        accountWithTransferables.update(augmentedTransferables)
    }
}
