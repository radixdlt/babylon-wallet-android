package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class PoolRedemptionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
) : PreviewTypeProcessor<DetailedManifestClass.PoolRedemption> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolRedemption): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(assets)

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        // TODO micbakos
//        val defaultDepositGuarantees = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
//        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
//        val from = summary.withdrawals.map { withdrawsPerAddress ->
//            withdrawsPerAddress.value.map { withdraw ->
//                val poolUnit = assets.find { it.resource.address == withdraw.address } as? PoolUnit
//                if (poolUnit == null) {
//                    summary.resolveDepositingAsset(withdraw, assets, defaultDepositGuarantees)
//                } else {
//                    val redemptions = classification.poolRedemptions.filter {
//                        it.poolUnitsResourceAddress == withdraw.address
//                    }
//                    val redemptionResourceAddresses = redemptions.first().redeemedResources.keys
//                    val poolUnitAmount = redemptions.find {
//                        it.poolUnitsResourceAddress == poolUnit.resourceAddress
//                    }?.poolUnitsAmount.orZero()
//                    Transferable.Withdrawing(
//                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
//                            amount = redemptions.map { it.poolUnitsAmount }.sumOf { it },
//                            unit = poolUnit.copy(
//                                stake = poolUnit.stake.copy(ownedAmount = poolUnitAmount)
//                            ),
//                            redemptionResourceAddresses.associateWith { contributedResourceAddress ->
//                                redemptions.mapNotNull {
//                                    it.redeemedResources[contributedResourceAddress]
//                                }.sumOf { it }
//                            }
//                        )
//                    )
//                }
//            }.toAccountWithTransferableResources(withdrawsPerAddress.key, involvedOwnedAccounts)
//        }.sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))

        return PreviewType.Transfer.Pool(
            from = withdraws,
            to = deposits,
            badges = badges,
            actionType = PreviewType.Transfer.Pool.ActionType.Redemption,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }
}
