package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.TrackedValidatorStake
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.plus
import com.radixdlt.sargon.extensions.toDecimal192
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.XrdResource
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

        return PreviewType.Transaction.Staking(
            from = withdraws,
            to = deposits.augmentWithStakes(classification.validatorStakes),
            badges = summary.resolveBadges(assets),
            validators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator },
            actionType = PreviewType.Transaction.Staking.ActionType.Stake,
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }

    private fun List<AccountWithTransferables>.augmentWithStakes(
        stakes: List<TrackedValidatorStake>
    ): List<AccountWithTransferables> = map { accountWithTransferableResources ->
        val transferables = accountWithTransferableResources.transferables.map tr@{ transferable ->
            val lsu = (transferable as? Transferable.FungibleType.LSU) ?: return@tr transferable

            var totalStaked = 0.toDecimal192()
            var totalStakedXrd = 0.toDecimal192()

            stakes.filter {
                it.liquidStakeUnitAddress == lsu.resourceAddress
            }.forEach { stake ->
                totalStaked += stake.liquidStakeUnitAmount
                totalStakedXrd += stake.xrdAmount
            }

            lsu.copy(
                xrdWorth = lsu.amount.calculateWith { decimal ->
                    lsu.asset.stakeValueXRD(
                        lsu = decimal,
                        totalStaked = totalStaked,
                        totalStakedInXrd = totalStakedXrd
                    ).orZero()
                }
            )
        }

        accountWithTransferableResources.update(transferables)
    }
}
