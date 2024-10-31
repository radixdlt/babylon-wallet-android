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
import com.radixdlt.sargon.extensions.address
import com.radixdlt.sargon.extensions.orZero
import com.radixdlt.sargon.extensions.sumOf
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

class PoolContributionProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : PreviewTypeProcessor<DetailedManifestClass.PoolContribution> {

    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolContribution): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(addresses = summary.involvedAddresses()).getOrThrow()
        val badges = summary.resolveBadges(assets)

        val (withdraws, deposits) = summary.resolveWithdrawsAndDeposits(
            onLedgerAssets = assets,
            profile = getProfileUseCase()
        )

        return PreviewType.Transfer.Pool(
            from = withdraws,
            to = deposits,
            badges = badges,
            actionType = PreviewType.Transfer.Pool.ActionType.Contribution,
            newlyCreatedNFTItems = summary.newlyCreatedNonFungibleItems()
        )
    }

    // TODO micbakos
    private fun ExecutionSummary.extractDeposits(
        classification: DetailedManifestClass.PoolContribution,
        assets: List<Asset>,
        defaultDepositGuarantee: Decimal192,
        involvedOwnedAccounts: List<Account>
    ): List<AccountWithTransferableResources> {
        val to = deposits.map { depositsPerAddress ->
            depositsPerAddress.value.map { deposit ->
                val poolUnit = assets.find { it.resource.address == deposit.address } as? PoolUnit
                if (poolUnit == null) {
                    resolveDepositingAsset(deposit, assets, defaultDepositGuarantee)
                } else {
                    val contributions = classification.poolContributions.filter {
                        it.poolUnitsResourceAddress == deposit.address
                    }
                    val contributedResourceAddresses = contributions.first().contributedResources.keys
                    val guaranteeType = deposit.guaranteeType(defaultDepositGuarantee)
                    val poolUnitAmount = contributions.find {
                        it.poolUnitsResourceAddress == poolUnit.resourceAddress
                    }?.poolUnitsAmount.orZero()
                    val contributionPerResource = contributedResourceAddresses.associateWith { contributedResourceAddress ->
                        contributions.mapNotNull {
                            it.contributedResources[contributedResourceAddress]
                        }.sumOf { it }
                    }
                    Transferable.Depositing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = contributions.map { it.poolUnitsAmount }.sumOf { it },
                            unit = poolUnit.copy(
                                stake = poolUnit.stake.copy(ownedAmount = poolUnitAmount)
                            ),
                            contributionPerResource = contributionPerResource
                        ),
                        guaranteeType = guaranteeType,
                    )
                }
            }.toAccountWithTransferableResources(depositsPerAddress.key, involvedOwnedAccounts)
        }
        return to
    }
}
