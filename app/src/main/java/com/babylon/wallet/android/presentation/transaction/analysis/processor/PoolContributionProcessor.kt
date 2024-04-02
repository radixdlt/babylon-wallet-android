package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.ResourceAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import kotlinx.coroutines.flow.first
import rdx.works.core.domain.assets.Asset
import rdx.works.core.domain.assets.PoolUnit
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class PoolContributionProcessor @Inject constructor(
    private val resolveAssetsFromAddressUseCase: ResolveAssetsFromAddressUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : PreviewTypeProcessor<DetailedManifestClass.PoolContribution> {

    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolContribution): PreviewType {
        val assets = resolveAssetsFromAddressUseCase(
            fungibleAddresses = summary.involvedFungibleAddresses(),
            nonFungibleIds = summary.involvedNonFungibleIds()
        ).getOrThrow()
        val defaultDepositGuarantee = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val involvedOwnedAccounts = summary.involvedOwnedAccounts(getProfileUseCase.accountsOnCurrentNetwork())
        val from = summary.toWithdrawingAccountsWithTransferableAssets(assets, involvedOwnedAccounts)
        val to = summary.extractDeposits(
            classification = classification,
            assets = assets,
            defaultDepositGuarantee = defaultDepositGuarantee,
            involvedOwnedAccounts = involvedOwnedAccounts
        ).sortedWith(AccountWithTransferableResources.Companion.Sorter(involvedOwnedAccounts))
        return PreviewType.Transfer.Pool(
            from = from,
            to = to,
            actionType = PreviewType.Transfer.Pool.ActionType.Contribution
        )
    }

    private fun ExecutionSummary.extractDeposits(
        classification: DetailedManifestClass.PoolContribution,
        assets: List<Asset>,
        defaultDepositGuarantee: Double,
        involvedOwnedAccounts: List<Network.Account>
    ): List<AccountWithTransferableResources> {
        val to = accountDeposits.map { depositsPerAddress ->
            depositsPerAddress.value.map { deposit ->
                val resourceAddress = deposit.resourceAddress
                val poolUnit = assets.find { it.resource.address == resourceAddress } as? PoolUnit
                if (poolUnit == null) {
                    resolveDepositingAsset(deposit, assets, defaultDepositGuarantee)
                } else {
                    val contributions = classification.poolContributions.filter {
                        it.poolUnitsResourceAddress.addressString() == resourceAddress.string
                    }
                    val contributedResourceAddresses = contributions.first().contributedResources.keys
                    val guaranteeType = deposit.guaranteeType(defaultDepositGuarantee)
                    val poolUnitAmount = contributions.find {
                        it.poolUnitsResourceAddress.addressString() == poolUnit.resourceAddress.string
                    }?.poolUnitsAmount?.asStr()?.toBigDecimalOrNull()
                    val contributionPerResource = contributedResourceAddresses.associate { contributedResourceAddress ->
                        ResourceAddress.init(contributedResourceAddress) to contributions.mapNotNull {
                            it.contributedResources[contributedResourceAddress]?.asStr()?.toBigDecimal()
                        }.sumOf { it }
                    }
                    Transferable.Depositing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = contributions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                            unit = poolUnit.copy(
                                stake = poolUnit.stake.copy(ownedAmount = poolUnitAmount)
                            ),
                            contributionPerResource = contributionPerResource
                        ),
                        guaranteeType = guaranteeType,
                    )
                }
            }.toAccountWithTransferableResources(AccountAddress.init(depositsPerAddress.key), involvedOwnedAccounts)
        }
        return to
    }
}
