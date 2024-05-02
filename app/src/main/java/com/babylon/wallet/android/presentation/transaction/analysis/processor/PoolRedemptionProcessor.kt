package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.sargon.DetailedManifestClass
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.sumOf
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.core.sargon.activeAccountsOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class PoolRedemptionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
) : PreviewTypeProcessor<DetailedManifestClass.PoolRedemption> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolRedemption): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses(),
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val defaultDepositGuarantees = getProfileUseCase().appPreferences.transaction.defaultDepositGuarantee
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase().activeAccountsOnCurrentNetwork)
        val to = summary.toDepositingAccountsWithTransferableAssets(
            allOwnedAccounts = involvedOwnedAccounts,
            involvedAssets = assets,
            defaultGuarantee = defaultDepositGuarantees
        )
        val from = summary.withdrawals.map { withdrawsPerAddress ->
            withdrawsPerAddress.value.map { withdraw ->
                val poolUnit = assets.find { it.resource.address == withdraw.address } as? PoolUnit
                if (poolUnit == null) {
                    summary.resolveDepositingAsset(withdraw, assets, defaultDepositGuarantees)
                } else {
                    val redemptions = classification.poolRedemptions.filter {
                        it.poolUnitsResourceAddress == withdraw.address
                    }
                    val redemptionResourceAddresses = redemptions.first().redeemedResources.keys
                    val poolUnitAmount = redemptions.find {
                        it.poolUnitsResourceAddress == poolUnit.resourceAddress
                    }?.poolUnitsAmount.orZero()
                    Transferable.Withdrawing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = redemptions.map { it.poolUnitsAmount }.sumOf { it },
                            unit = poolUnit.copy(
                                stake = poolUnit.stake.copy(ownedAmount = poolUnitAmount)
                            ),
                            redemptionResourceAddresses.associateWith { contributedResourceAddress ->
                                redemptions.mapNotNull {
                                    it.redeemedResources[contributedResourceAddress]
                                }.sumOf { it }
                            }
                        )
                    )
                }
            }.toAccountWithTransferableResources(withdrawsPerAddress.key, involvedOwnedAccounts)
        }.sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))
        return PreviewType.Transfer.Pool(
            from = from,
            to = to,
            actionType = PreviewType.Transfer.Pool.ActionType.Redemption
        )
    }
}
