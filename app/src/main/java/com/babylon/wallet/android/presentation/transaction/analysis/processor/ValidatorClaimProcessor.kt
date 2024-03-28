package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
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
        val xrdAddress = XrdResource.address(networkId.value)
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
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val toAccounts = summary.toDepositingAccountsWithTransferableAssets(
            assets,
            involvedOwnedAccounts,
            defaultDepositGuarantees
        )
        val fromAccounts = extractWithdrawals(
            executionSummary = summary,
            assets = assets,
            defaultDepositGuarantees = defaultDepositGuarantees,
            involvedOwnedAccounts = involvedOwnedAccounts
        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))
        return PreviewType.Transfer.Staking(
            validators = involvedValidators.toList(),
            from = fromAccounts,
            to = toAccounts,
            actionType = PreviewType.Transfer.Staking.ActionType.ClaimStake
        )
    }

    private fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        assets: List<Asset>,
        defaultDepositGuarantees: Double,
        involvedOwnedAccounts: List<Network.Account>
    ): List<AccountWithTransferableResources> {
        val stakeClaimNfts = assets.filterIsInstance<Asset.NonFungible>().map { it.resource.items }.flatten()
        return executionSummary.accountWithdraws.map { claimsPerAddress ->
            claimsPerAddress.value.map { resourceIndicator ->
                val resourceAddress = resourceIndicator.resourceAddress
                val asset = assets.find { it.resource.resourceAddress == resourceAddress } ?: error("No asset found")
                if (asset is StakeClaim) {
                    val nonFungibleIndicator = resourceIndicator as? ResourceIndicator.NonFungible
                        ?: error("No non-fungible resource claim found")
                    val items = nonFungibleIndicator.nonFungibleLocalIds.map { localId ->
                        val claimAmount = stakeClaimNfts.find {
                            resourceAddress == it.collectionAddress && localId == it.localId
                        }?.claimAmountXrd ?: BigDecimal.ZERO
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceAddress,
                            localId = localId
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
            }.toAccountWithTransferableResources(claimsPerAddress.key, involvedOwnedAccounts)
        }
    }
}
