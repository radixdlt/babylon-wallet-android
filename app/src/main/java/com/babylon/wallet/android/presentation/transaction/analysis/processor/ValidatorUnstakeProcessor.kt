package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
import com.babylon.wallet.android.domain.model.assets.ValidatorDetail
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.XrdResource
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.NonFungibleGlobalId
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
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
        val allOwnedAccounts = summary.allOwnedAccounts(getProfileUseCase)
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map { it.validator }
        val fromAccounts = summary.toWithdrawingAccountsWithTransferableAssets(assets, allOwnedAccounts)
        val toAccounts = classification.extractDeposits(summary, getProfileUseCase, assets, involvedValidators)
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
        involvedValidators: List<ValidatorDetail>
    ) = executionSummary.accountDeposits.map { claimsPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val depositingTransferables = claimsPerAddress.value.map { claimedResource ->
            val resourceAddress = claimedResource.resourceAddress
            val resource =
                assets.find { it.resource.resourceAddress == resourceAddress }?.resource
                    ?: error("No resource found")
            val validator =
                involvedValidators.find { resource.validatorAddress == it.address }
            if (validator == null) {
                executionSummary.resolveDepositingAsset(claimedResource, assets, defaultDepositGuarantees)
            } else {
                resource as? Resource.NonFungibleResource ?: error("No fungible resource found")
                claimedResource as? ResourceIndicator.NonFungible
                    ?: error("No non-fungible indicator found")
                val stakeClaimNftItems = claimedResource.indicator.nonFungibleLocalIds.map { localId ->
                    val globalId = NonFungibleGlobalId.fromParts(claimedResource.resourceAddress, localId)
                    val claimAmount =
                        claimsNonFungibleData.find { it.nonFungibleGlobalId.asStr() == globalId.asStr() }?.data?.claimAmount?.asStr()
                            ?.toBigDecimal()
                            ?: error("No claim amount found")
                    Resource.NonFungibleResource.Item(
                        collectionAddress = resourceAddress,
                        localId = Resource.NonFungibleResource.Item.ID.from(localId)
                    ) to claimAmount
                }
                val guaranteeType = claimedResource.guaranteeType(defaultDepositGuarantees)
                Transferable.Depositing(
                    transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                        claim = StakeClaim(
                            nonFungibleResource = resource.copy(items = stakeClaimNftItems.map { it.first }),
                            validator = validator
                        ),
                        xrdWorthPerNftItem = stakeClaimNftItems.associate {
                            it.first.localId.displayable to it.second
                        },
                        isNewlyCreated = true
                    ),
                    guaranteeType = guaranteeType
                )
            }
        }
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = depositingTransferables
        )
    }
}
