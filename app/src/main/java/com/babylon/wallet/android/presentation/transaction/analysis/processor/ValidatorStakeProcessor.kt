package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.model.FungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.model.TransferableX
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.TrackedValidatorStake
import com.radixdlt.sargon.extensions.div
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.times
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.roundedWith
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ValidatorStakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorStake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorStake): PreviewType {
        val profile = getProfileUseCase()
        val xrdAddress = XrdResource.address(profile.currentGateway.network.id)

        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = profile
        )

        return PreviewType.Transfer.Staking(
            from = withdraws,
            to = deposits.augmentWithStakes(classification.validatorStakes),
            badges = summary.resolveBadges(assets),
            validators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator },
            actionType = PreviewType.Transfer.Staking.ActionType.Stake,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }

    private fun List<AccountWithTransferableResources>.augmentWithStakes(
        stakes: List<TrackedValidatorStake>
    ): List<AccountWithTransferableResources> = map { accountWithTransferableResources ->
        val transferables = accountWithTransferableResources.resources.map tr@{ transferable ->
            val lsu = (transferable as? TransferableX.FungibleType.LSU) ?: return@tr transferable

            var totalStakeForLSU = 0.toDecimal192()
            var totalXrdWorthForLSU = 0.toDecimal192()

            stakes.filter {
                it.liquidStakeUnitAddress == lsu.resourceAddress
            }.forEach { stake ->
                totalStakeForLSU += stake.liquidStakeUnitAmount
                totalXrdWorthForLSU += stake.xrdAmount
            }

            val lsuAmount = when (lsu.amount) {
                is FungibleAmount.Exact -> lsu.amount.amount
                is FungibleAmount.Predicted -> lsu.amount.amount
                else -> TODO() // Cannot calculate
            }

            val xrdWorth = ((lsuAmount / totalStakeForLSU) * totalXrdWorthForLSU)
                .roundedWith(lsu.asset.fungibleResource.divisibility)

            lsu.copy(xrdWorth = xrdWorth)
        }

        accountWithTransferableResources.update(transferables)
    }
}
