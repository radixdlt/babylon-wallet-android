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
import com.radixdlt.ret.ResourceIndicator
import com.radixdlt.ret.nonFungibleLocalIdAsStr
import kotlinx.coroutines.flow.first
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.currentNetwork
import java.math.BigDecimal
import javax.inject.Inject

class ValidatorClaimProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorClaim> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorClaim): PreviewType {
        val networkId = requireNotNull(getProfileUseCase.currentNetwork()?.knownNetworkId)
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses() + xrdAddress,
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map {
            it.validator
        }.toSet() + assets.filterIsInstance<StakeClaim>().map {
            it.validator
        }.toSet()
        val involvedAccountAddresses = summary.accountWithdraws.keys + summary.accountDeposits.keys
        val allOwnedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            involvedAccountAddresses.contains(it.address)
        }
        val toAccounts = summary.toDepositingAccountsWithTransferableAssets(assets, allOwnedAccounts, defaultDepositGuarantees)
        val fromAccounts = extractWithdrawals(
            executionSummary = summary,
            assets = assets,
            involvedValidators = involvedValidators.toList(),
            defaultDepositGuarantees = defaultDepositGuarantees
        )
        return PreviewType.Transfer.Staking(
            validators = involvedValidators.toList(),
            from = fromAccounts,
            to = toAccounts,
            actionType = PreviewType.Transfer.Staking.ActionType.ClaimStake
        )
    }

    private suspend fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        assets: List<Asset>,
        involvedValidators: List<ValidatorDetail>,
        defaultDepositGuarantees: Double
    ): List<AccountWithTransferableResources.Owned> {
        val stakeClaimNfts = assets.filterIsInstance<Asset.NonFungible>().map { it.resource.items }.flatten()
        return executionSummary.accountWithdraws.map { claimsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
            val withdrawingNfts =
                claimsPerAddress.value.groupBy { it.resourceAddress }
                    .map { resourceClaim ->
                        val resourceAddress = resourceClaim.key
                        val resource =
                            assets.find { it.resource.resourceAddress == resourceAddress }?.resource
                                ?: error("No resource found")
                        val validatorAddress = resource.validatorAddress
                        if (validatorAddress == null) {
                            executionSummary.resolveDepositingAsset(resourceClaim.value.first(), assets, defaultDepositGuarantees)
                        } else {
                            resource as? Resource.NonFungibleResource
                                ?: error("No non-fungible resource found")
                            val claims = resourceClaim.value.filterIsInstance<ResourceIndicator.NonFungible>()
                            val validator = involvedValidators.find { validatorAddress == it.address } ?: error("No validator found")
                            val items = claims.map { resourceIndicator ->
                                resourceIndicator.localIds.map { localId ->
                                    val claimAmount = stakeClaimNfts.find {
                                        resourceAddress == it.collectionAddress && localId == nonFungibleLocalIdAsStr(it.localId.toRetId())
                                    }?.claimAmountXrd ?: BigDecimal.ZERO
                                    Resource.NonFungibleResource.Item(
                                        collectionAddress = resourceAddress,
                                        localId = Resource.NonFungibleResource.Item.ID.from(localId)
                                    ) to claimAmount
                                }
                            }.flatten()
                            Transferable.Withdrawing(
                                transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                                    claim = StakeClaim(
                                        nonFungibleResource = resource.copy(items = items.map { it.first }),
                                        validator = validator
                                    ),
                                    xrdWorthPerNftItem = items.associate {
                                        it.first.localId.displayable to it.second
                                    }
                                )
                            )
                        }
                    }
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = withdrawingNfts
            )
        }
    }
}
