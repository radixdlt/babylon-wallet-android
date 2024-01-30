package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.Asset
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.domain.usecases.assets.ResolveAssetsFromAddressUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.flow.first
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
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
        val accountsWithdrawnFrom = summary.accountWithdraws.keys
        val ownedAccountsWithdrawnFrom = getProfileUseCase.accountsOnCurrentNetwork().filter {
            accountsWithdrawnFrom.contains(it.address)
        }
        val from = summary.extractWithdraws(ownedAccountsWithdrawnFrom, assets)
        val to = summary.extractDeposits(classification, assets, defaultDepositGuarantee)
        return PreviewType.Transfer.Pool(
            from = from,
            to = to,
            actionType = PreviewType.Transfer.Pool.ActionType.Contribution
        )
    }

    private suspend fun ExecutionSummary.extractDeposits(
        classification: DetailedManifestClass.PoolContribution,
        assets: List<Asset>,
        defaultDepositGuarantee: Double
    ): List<AccountWithTransferableResources.Owned> {
        val involvedPools = assets.filterIsInstance<PoolUnit>().mapNotNull { it.pool }
        val to = accountDeposits.map { depositsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAddress.key) ?: error("No account found")
            val deposits = depositsPerAddress.value.map { deposit ->
                val resourceAddress = deposit.resourceAddress
                val contributions = classification.poolContributions.filter {
                    it.poolUnitsResourceAddress.addressString() == resourceAddress
                }
                if (contributions.isEmpty()) {
                    resolveGeneralAsset(deposit, this, assets, defaultDepositGuarantee)
                } else {
                    val pool = involvedPools.find { it.address == contributions.first().poolAddress.addressString() }
                        ?: error("No pool found")
                    val poolResource = assets.find {
                        it.resource.resourceAddress == pool.metadata.poolUnit()
                    }?.resource as? Resource.FungibleResource
                        ?: error("No pool resource found")
                    val contributedResourceAddresses = contributions.first().contributedResources.keys
                    val guaranteeType = deposit.guaranteeType(defaultDepositGuarantee)
                    val poolUnitAmount = contributions.find {
                        it.poolUnitsResourceAddress.addressString() == poolResource.resourceAddress
                    }?.poolUnitsAmount?.asStr()?.toBigDecimalOrNull()
                    Transferable.Depositing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = contributions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                            unit = PoolUnit(
                                stake = poolResource.copy(ownedAmount = poolUnitAmount),
                                pool = pool
                            ),
                            contributionPerResource = contributedResourceAddresses.associateWith { contributedResourceAddress ->
                                contributions.mapNotNull { it.contributedResources[contributedResourceAddress]?.asStr()?.toBigDecimal() }
                                    .sumOf { it }
                            }
                        ),
                        guaranteeType = guaranteeType,
                    )
                }
            }
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = deposits
            )
        }
        return to
    }

    private fun resolveGeneralAsset(
        deposit: ResourceIndicator,
        summary: ExecutionSummary,
        involvedAssets: List<Asset>,
        defaultDepositGuarantee: Double
    ): Transferable.Depositing {
        val asset = if (deposit.isNewlyCreated(summary = summary)) {
            deposit.toNewlyCreatedTransferableAsset(deposit.newlyCreatedMetadata(summary = summary))
        } else {
            deposit.toTransferableAsset(involvedAssets)
        }

        return Transferable.Depositing(
            transferable = asset,
            guaranteeType = deposit.guaranteeType(defaultDepositGuarantee)
        )
    }

    private fun ExecutionSummary.extractWithdraws(allOwnedAccounts: List<Network.Account>, assets: List<Asset>) =
        accountWithdraws.entries.map { transferEntry ->
            val accountOnNetwork = allOwnedAccounts.find { it.address == transferEntry.key }

            val withdrawing = transferEntry.value.map { resourceIndicator ->
                Transferable.Withdrawing(resourceIndicator.toTransferableAsset(assets))
            }
            accountOnNetwork?.let { account ->
                AccountWithTransferableResources.Owned(
                    account = account,
                    resources = withdrawing
                )
            } ?: AccountWithTransferableResources.Other(
                address = transferEntry.key,
                resources = withdrawing
            )
        }
}
