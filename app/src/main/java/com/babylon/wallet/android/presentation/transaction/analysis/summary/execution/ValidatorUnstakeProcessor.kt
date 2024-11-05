package com.babylon.wallet.android.presentation.transaction.analysis.summary.execution

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.model.NonFungibleAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.UnstakeData
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.domain.resources.metadata.Metadata
import rdx.works.core.domain.resources.metadata.MetadataType
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ValidatorUnstakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorUnstake> {

    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorUnstake): PreviewType {
        val profile = getProfileUseCase()
        val xrdAddress = XrdResource.address(profile.currentGateway.network.id)
        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transaction.Staking(
            from = withdraws,
            to = deposits.augmentWithClaimNFTs(claimsData = classification.claimsNonFungibleData),
            badges = summary.resolveBadges(assets),
            validators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator },
            actionType = PreviewType.Transaction.Staking.ActionType.Unstake,
            newlyCreatedGlobalIds = summary.newlyCreatedNonFungibles
        )
    }

    // Adds claim epoch and claim amount data to NFTs related to this transaction
    private fun List<AccountWithTransferables>.augmentWithClaimNFTs(
        claimsData: Map<NonFungibleGlobalId, UnstakeData>
    ): List<AccountWithTransferables> = map { accountWithTransferables ->
        val transferables = accountWithTransferables.transferables.map tr@{ transferable ->
            val transferableClaim = (transferable as? Transferable.NonFungibleType.StakeClaim) ?: return@tr transferable

            val nfts = transferableClaim.amount.certain.map nf@{ nft ->
                val data = claimsData[nft.globalId] ?: return@nf nft

                nft.copy(
                    metadata = listOf(
                        Metadata.Primitive(
                            ExplicitMetadataKey.NAME.key,
                            data.name,
                            MetadataType.String
                        ),
                        Metadata.Primitive(
                            ExplicitMetadataKey.CLAIM_AMOUNT.key,
                            data.claimAmount.string,
                            MetadataType.Decimal
                        ),
                        Metadata.Primitive(
                            ExplicitMetadataKey.CLAIM_EPOCH.key,
                            data.claimEpoch.toString(),
                            MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
                        )
                    )
                )
            }

            transferableClaim.copy(amount = NonFungibleAmount(nfts))
        }

        accountWithTransferables.update(transferables)
    }
}
