package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferableResources
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Decimal192
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.ResourceIndicator
import com.radixdlt.sargon.ResourceOrNonFungible
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.orZero
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.LiquidStakeUnit
import rdx.works.core.domain.assets.StakeClaim
import rdx.works.core.domain.resources.Resource
import rdx.works.core.domain.resources.XrdResource
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.core.sargon.currentGateway
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class ValidatorClaimProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase
) : PreviewTypeProcessor<DetailedManifestClass.ValidatorClaim> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.ValidatorClaim): PreviewType {
        val networkId = getProfileUseCase().currentGateway.network.id
        val xrdAddress = XrdResource.address(networkId)
        val assets = resolveAssetsFromAddressUseCase(
            addresses = summary.involvedAddresses() + ResourceOrNonFungible.Resource(xrdAddress)
        ).getOrThrow()
        val badges = summary.resolveBadges(assets)
        val involvedValidators = assets.filterIsInstance<LiquidStakeUnit>().map {
            it.validator
        }.toSet() + assets.filterIsInstance<StakeClaim>().map {
            it.validator
        }.toSet()

        // TODO micbakos
//        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
//        val defaultDepositGuarantees = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
//        val toAccounts = summary.toDepositingAccountsWithTransferableAssets(
//            assets,
//            involvedOwnedAccounts,
//            defaultDepositGuarantees
//        )
//        val fromAccounts = extractWithdrawals(
//            executionSummary = summary,
//            assets = assets,
//            defaultDepositGuarantees = defaultDepositGuarantees,
//            involvedOwnedAccounts = involvedOwnedAccounts
//        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transfer.Staking(
            validators = involvedValidators.toList(),
            from = withdraws,
            to = deposits,
            badges = badges,
            actionType = PreviewType.Transfer.Staking.ActionType.ClaimStake,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }

    private fun extractWithdrawals(
        executionSummary: ExecutionSummary,
        assets: List<Asset>,
        defaultDepositGuarantees: Decimal192,
        involvedOwnedAccounts: List<Account>
    ): List<AccountWithTransferableResources> {
        val stakeClaimNfts = assets.filterIsInstance<Asset.NonFungible>().map { it.resource.items }.flatten()
        return executionSummary.withdrawals.map { claimsPerAddress ->
            claimsPerAddress.value.map { resourceIndicator ->
                val asset = assets.find { it.resource.address == resourceIndicator.address } ?: error("No asset found")
                if (asset is StakeClaim) {
                    val nonFungibleIndicator = resourceIndicator as? ResourceIndicator.NonFungible
                        ?: error("No non-fungible resource claim found")
                    val items = nonFungibleIndicator.nonFungibleLocalIds.map { localId ->
                        val claimAmount = stakeClaimNfts.find {
                            resourceIndicator.address == it.collectionAddress && localId == it.localId
                        }?.claimAmountXrd.orZero()
                        Resource.NonFungibleResource.Item(
                            collectionAddress = resourceIndicator.address,
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
                                it.first.localId to it.second
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
