package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.LiquidStakeUnit
import com.babylon.wallet.android.domain.model.assets.StakeClaim
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
        defaultDepositGuarantees: Double
    ): List<AccountWithTransferableResources.Owned> {
        val stakeClaimNfts = assets.filterIsInstance<Asset.NonFungible>().map { it.resource.items }.flatten()
        return executionSummary.accountWithdraws.map { claimsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(claimsPerAddress.key) ?: error("No account found")
            val withdrawingTransferables =
                claimsPerAddress.value.map { resourceIndicator ->
                    val resourceAddress = resourceIndicator.resourceAddress
                    val asset = assets.find { it.resource.resourceAddress == resourceAddress } ?: error("No asset found")
                    if (asset is StakeClaim) {
                        resourceIndicator as? ResourceIndicator.NonFungible ?: error("No non-fungible resource claim found")
                        val items = resourceIndicator.localIds.map { localId ->
                            val claimAmount = stakeClaimNfts.find {
                                resourceAddress == it.collectionAddress && localId == nonFungibleLocalIdAsStr(it.localId.toRetId())
                            }?.claimAmountXrd ?: BigDecimal.ZERO
                            Resource.NonFungibleResource.Item(
                                collectionAddress = resourceAddress,
                                localId = Resource.NonFungibleResource.Item.ID.from(localId)
                            ) to claimAmount
                        }
                        Transferable.Withdrawing(
                            transferable = TransferableAsset.NonFungible.StakeClaimAssets(
                                claim = StakeClaim(
                                    nonFungibleResource = asset.resource.copy(items = items.map { it.first }),
                                    validator = asset.validator
                                ),
                                xrdWorthPerNftItem = items.associate {
                                    it.first.localId.displayable to it.second
                                }
                            )
                        )
                    } else {
                        executionSummary.resolveDepositingAsset(resourceIndicator, assets, defaultDepositGuarantees)
                    }
                }
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = withdrawingTransferables
            )
        }
    }
}
