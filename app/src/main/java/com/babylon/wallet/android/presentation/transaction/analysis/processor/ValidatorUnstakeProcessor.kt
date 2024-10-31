package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.NonFungibleGlobalId
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.string
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.ExplicitMetadataKey
import rdx.works.core.domain.resources.Resource
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
        val networkId = getProfileUseCase().currentGateway.network.id
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()
        val badges = summary.resolveBadges(assets)
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }

        // TODO micbakos
//        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
//        val fromAccounts = summary.toWithdrawingAccountsWithTransferableAssets(assets, involvedOwnedAccounts)
//        val toAccounts = classification.extractDeposits(
//            executionSummary = summary,
//            getProfileUseCase = getProfileUseCase,
//            assets = assets,
//            involvedOwnedAccounts = involvedOwnedAccounts
//        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transfer.Staking(
            from = withdraws,
            to = deposits,
            badges = badges,
            validators = involvedValidators,
            actionType = PreviewType.Transfer.Staking.ActionType.Unstake,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }

    private suspend fun DetailedManifestClass.ValidatorUnstake.extractDeposits(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        assets: List<Asset>,
        involvedOwnedAccounts: List<Account>
    ) = executionSummary.deposits.map { claimsPerAddress ->
        val defaultDepositGuarantees = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
        claimsPerAddress.value.map { claimedResource ->
            val asset = assets.find { it.resource.address == claimedResource.address } ?: error("No resource found")
            if (asset is StakeClaim) {
                claimedResource as? ResourceIndicator.NonFungible
                    ?: error("No non-fungible indicator found")
                val stakeClaimNftItems = claimedResource.nonFungibleLocalIds.map { localId ->
                    val globalId = NonFungibleGlobalId(
                        resourceAddress = claimedResource.resourceAddress,
                        nonFungibleLocalId = localId,
                    )
                    val claimNFTData = claimsNonFungibleData[globalId] ?: error("No claim data found")
                    val claimAmount = claimNFTData.claimAmount
                    val claimEpoch = claimNFTData.claimEpoch
                    Resource.NonFungibleResource.Item(
                        collectionAddress = claimedResource.resourceAddress,
                        localId = localId,
                        metadata = listOf(
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                claimAmount.string,
                                MetadataType.Decimal
                            ),
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIM_EPOCH.key,
                                claimEpoch.toString(),
                                MetadataType.Integer(signed = false, size = MetadataType.Integer.Size.LONG)
                            )
                        ),
                    ) to claimAmount
                }
                val guaranteeType = claimedResource.guaranteeType(defaultDepositGuarantees)
                Transferable.Depositing(
                    transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                        claim = StakeClaim(
                            nonFungibleResource = asset.resource.copy(items = stakeClaimNftItems.map { it.first }),
                            validator = asset.validator
                        ),
                        xrdWorthPerNftItem = stakeClaimNftItems.associate {
                            it.first.localId to it.second
                        },
                        isNewlyCreated = true
                    ),
                    guaranteeType = guaranteeType
                )
            } else {
                executionSummary.resolveDepositingAsset(claimedResource, assets, defaultDepositGuarantees)
            }
        }.toAccountWithTransferableResources(claimsPerAddress.key, involvedOwnedAccounts)
    }
}
