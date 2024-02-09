package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.data.gateway.model.ExplicitMetadataKey
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.model.resources.metadata.Metadata
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataType
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import javax.inject.Inject

class ValidatorUnstakeProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorUnstake> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorUnstake): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses() + xrdAddress,
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }
        val fromAccounts = summary.toWithdrawingAccountsWithTransferableAssets(assets, involvedOwnedAccounts)
        val toAccounts = classification.extractDeposits(
            executionSummary = summary,
            getProfileUseCase = getProfileUseCase,
            assets = assets,
            involvedOwnedAccounts = involvedOwnedAccounts
        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))
        return PreviewType.Transfer.Staking(
            from = fromAccounts,
            to = toAccounts,
            validators = involvedValidators,
            actionType = PreviewType.Transfer.Staking.ActionType.Unstake
        )
    }

    private suspend fun DetailedManifestClass.ValidatorUnstake.extractDeposits(
        executionSummary: ExecutionSummary,
        getProfileUseCase: GetProfileUseCase,
        assets: List<Asset>,
        involvedOwnedAccounts: List<Network.Account>
    ) = executionSummary.accountDeposits.map { claimsPerAddress ->
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        claimsPerAddress.value.map { claimedResource ->
            val resourceAddress = claimedResource.resourceAddress
            val asset = assets.find { it.resource.resourceAddress == resourceAddress } ?: error("No resource found")
            if (asset is StakeClaim) {
                claimedResource as? ResourceIndicator.NonFungible
                    ?: error("No non-fungible indicator found")
                val stakeClaimNftItems = claimedResource.indicator.nonFungibleLocalIds.map { localId ->
                    val globalId = NonFungibleGlobalId.fromParts(claimedResource.resourceAddress, localId)
                    val claimNFTData = claimsNonFungibleData.find { it.nonFungibleGlobalId.asStr() == globalId.asStr() }?.data
                        ?: error("No claim data found")
                    val claimAmount = claimNFTData.claimAmount.asStr().toBigDecimal()
                    val claimEpoch = claimNFTData.claimEpoch
                    Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress,
                        localId = Resource.NonFungibleResource.Item.ID.from(localId),
                        metadata = listOf(
                            Metadata.Primitive(
                                ExplicitMetadataKey.CLAIM_AMOUNT.key,
                                claimAmount.toPlainString(),
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
                            it.first.localId.displayable to it.second
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
