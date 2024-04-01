package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
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
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val to = summary.toDepositingAccountsWithTransferableAssets(
            allOwnedAccounts = involvedOwnedAccounts,
            involvedAssets = assets,
            defaultGuarantee = defaultDepositGuarantees
        )
        val from = summary.accountWithdraws.map { withdrawsPerAddress ->
            withdrawsPerAddress.value.map { withdraw ->
                val resourceAddress = withdraw.resourceAddress
                val poolUnit = assets.find { it.resource.address == resourceAddress } as? PoolUnit
                if (poolUnit == null) {
                    summary.resolveDepositingAsset(withdraw, assets, defaultDepositGuarantees)
                } else {
                    val redemptions = classification.poolRedemptions.filter {
                        it.poolUnitsResourceAddress.addressString() == resourceAddress.string
                    }
                    val redemptionResourceAddresses = redemptions.first().redeemedResources.keys
                    val poolUnitAmount = redemptions.find {
                        it.poolUnitsResourceAddress.addressString() == poolUnit.resourceAddress.string
                    }?.poolUnitsAmount?.asStr()?.toBigDecimalOrNull()
                    Transferable.Withdrawing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = redemptions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                            unit = poolUnit.copy(
                                stake = poolUnit.stake.copy(ownedAmount = poolUnitAmount)
                            ),
                            redemptionResourceAddresses.associate { contributedResourceAddress ->
                                ResourceAddress.init(contributedResourceAddress) to redemptions.mapNotNull {
                                    it.redeemedResources[contributedResourceAddress]?.asStr()?.toBigDecimal()
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
