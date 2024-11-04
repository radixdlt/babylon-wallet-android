package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.TrackedPoolRedemption
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class PoolRedemptionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
) : PreviewTypeProcessor<DetailedManifestClass.PoolRedemption> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolRedemption): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(assets)

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transaction.Pool(
            from = withdraws.augmentWithRedemptions(redemptions = classification.poolRedemptions),
            to = deposits,
            badges = badges,
            actionType = PreviewType.Transaction.Pool.ActionType.Redemption,
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }

    private fun List<AccountWithTransferables>.augmentWithRedemptions(
        redemptions: List<TrackedPoolRedemption>
    ): List<AccountWithTransferables> = map { accountWithTransferables ->
        val augmentedTransferables = accountWithTransferables.transferables.map tr@{ transferable ->
            val poolUnit = (transferable as? Transferable.FungibleType.PoolUnit) ?: return@tr transferable

            var totalPoolUnitAmount = 0.toDecimal192()
            val redemptionsPerResource = mutableMapOf<ResourceAddress, Decimal192>()

            redemptions
                .filter {
                    it.poolUnitsResourceAddress == poolUnit.resourceAddress
                }.forEach { redemption ->
                    redemption.redeemedResources.forEach { (address, amount) ->
                        val currentAmount = redemptionsPerResource.getOrDefault(address, 0.toDecimal192())

                        redemptionsPerResource[address] = currentAmount + amount
                    }

                    totalPoolUnitAmount += redemption.poolUnitsAmount
                }

            val newAmount = when (poolUnit.amount) {
                is FungibleAmount.Exact -> FungibleAmount.Exact(totalPoolUnitAmount)
                is FungibleAmount.Predicted -> poolUnit.amount.copy(
                    estimated = totalPoolUnitAmount
                )
                else -> FungibleAmount.Exact(totalPoolUnitAmount)
            }

            poolUnit.copy(amount = newAmount)
        }

        accountWithTransferables.update(augmentedTransferables)
    }
}
