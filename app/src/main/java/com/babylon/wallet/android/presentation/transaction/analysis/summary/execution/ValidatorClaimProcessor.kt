package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceOrNonFungible
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ValidatorClaimProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorClaim> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorClaim): PreviewType {
        val profile = getProfileUseCase()
        val xrdAddress = XrdResource.address(profile.currentGateway.network.id)
        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()

        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map {
            it.validator
        }.toSet() + assets.filterIsInstance<StakeClaim>().map {
            it.validator
        }.toSet()

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transaction(
            from = withdraws,
            to = deposits,
            badges = summary.resolveBadges(assets),
            involvedComponents = PreviewType.Transaction.InvolvedComponents.Validators(
                validators = involvedValidators,
                actionType = PreviewType.Transaction.InvolvedComponents.Validators.ActionType.ClaimStake
            ),
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }
}
