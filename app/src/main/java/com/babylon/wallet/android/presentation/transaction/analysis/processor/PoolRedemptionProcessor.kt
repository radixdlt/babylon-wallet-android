package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolDetailsUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import javax.inject.Inject

class PoolRedemptionProcessor @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getPoolDetailsUseCase: GetPoolDetailsUseCase,
    private val resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
) : PreviewTypeProcessor<DetailedManifestClass.PoolRedemption> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolRedemption): PreviewType {
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses).getOrThrow()
        val involvedPools = getPoolDetailsUseCase(classification.poolAddresses.map { it.addressString() }.toSet()).getOrThrow()
        val poolsToDapps = involvedPools.resolveDApps(resolveDAppInTransactionUseCase)
        val accountsWithdrawnFrom = summary.accountDeposits.keys
        val ownedAccountsWithdrawnFrom = getProfileUseCase.accountsOnCurrentNetwork().filter {
            accountsWithdrawnFrom.contains(it.address)
        }
        val to = summary.extractDeposits(ownedAccountsWithdrawnFrom, resources)
        val from = summary.accountWithdraws.map { withdrawsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(withdrawsPerAddress.key) ?: error("No account found")
            val deposits = withdrawsPerAddress.value.map { withdraw ->
                val resourceAddress = withdraw.resourceAddress
                val redemptions = classification.poolRedemptions.filter {
                    it.poolUnitsResourceAddress.addressString() == resourceAddress
                }
                val pool = involvedPools.find { it.address == redemptions.first().poolAddress.addressString() }
                    ?: error("No pool found")
                val poolResource = resources.find { it.resourceAddress == pool.metadata.poolUnit() } as? Resource.FungibleResource
                    ?: error("No pool resource found")
                val redemptionResourceAddresses = redemptions.first().redeemedResources.keys
                Transferable.Withdrawing(
                    transferable = TransferableAsset.Fungible.PoolUnitAsset(
                        amount = redemptions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                        PoolUnit(
                            stake = poolResource,
                            pool = pool.copy(associatedDApp = poolsToDapps[pool])
                        ),
                        redemptionResourceAddresses.associateWith { contributedResourceAddress ->
                            redemptions.mapNotNull { it.redeemedResources[contributedResourceAddress]?.asStr()?.toBigDecimal() }
                                .sumOf { it }
                        }
                    )
                )
            }
            AccountWithTransferableResources.Owned(
                account = ownedAccount,
                resources = deposits
            )
        }
        return PreviewType.Transfer.Pool(
            from = from,
            to = to,
            poolsWithAssociatedDapps = poolsToDapps,
            actionType = PreviewType.Transfer.Pool.ActionType.Redemption
        )
    }

    private fun ExecutionSummary.extractDeposits(allOwnedAccounts: List<Network.Account>, resources: List<Resource>) =
        accountDeposits.entries.map { transferEntry ->
            val accountOnNetwork = allOwnedAccounts.find { it.address == transferEntry.key }

            val withdrawing = transferEntry.value.map { resourceIndicator ->
                Transferable.Depositing(resourceIndicator.toTransferableResource(resources))
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
