package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.DApp
import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.dAppDefinition
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetPoolDetailsUseCase
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
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getPoolDetailsUseCase: GetPoolDetailsUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
) : PreviewTypeProcessor<DetailedManifestClass.PoolContribution> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.PoolContribution): PreviewType {
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses).getOrThrow()
        val involvedPools = getPoolDetailsUseCase(classification.poolAddresses.map { it.addressString() }.toSet()).getOrThrow()
        val poolsToDapps = involvedPools.resolveDApps(resolveDAppInTransactionUseCase)
        val defaultDepositGuarantees = getProfileUseCase.invoke().first().appPreferences.transaction.defaultDepositGuarantee
        val accountsWithdrawnFrom = summary.accountWithdraws.keys
        val ownedAccountsWithdrawnFrom = getProfileUseCase.accountsOnCurrentNetwork().filter {
            accountsWithdrawnFrom.contains(it.address)
        }
        val from = summary.extractWithdraws(ownedAccountsWithdrawnFrom, resources)
        val to = summary.accountDeposits.map { depositsPerAddress ->
            val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAddress.key) ?: error("No account found")
            val deposits = depositsPerAddress.value.mapNotNull { deposit ->
                val resourceAddress = deposit.resourceAddress
                val contributions = classification.poolContributions.filter {
                    it.poolUnitsResourceAddress.addressString() == resourceAddress
                }
                if (contributions.isEmpty()) {
                    null
                } else {
                    val pool = involvedPools.find { it.address == contributions.first().poolAddress.addressString() }
                        ?: error("No pool found")
                    val poolResource = resources.find { it.resourceAddress == pool.metadata.poolUnit() } as? Resource.FungibleResource
                        ?: error("No pool resource found")
                    val contributedResourceAddresses = contributions.first().contributedResources.keys
                    val guaranteeType = (deposit as? ResourceIndicator.Fungible)?.guaranteeType(defaultDepositGuarantees)
                        ?: GuaranteeType.Guaranteed
                    Transferable.Depositing(
                        transferable = TransferableAsset.Fungible.PoolUnitAsset(
                            amount = contributions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                            unit = PoolUnit(
                                stake = poolResource,
                                pool = pool
                            ),
                            contributionPerResource = contributedResourceAddresses.associateWith { contributedResourceAddress ->
                                contributions.mapNotNull { it.contributedResources[contributedResourceAddress]?.asStr()?.toBigDecimal() }
                                    .sumOf { it }
                            },
                            associatedDapp = poolsToDapps[pool]
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
        return PreviewType.Transfer.Pool(
            from = from,
            to = to,
            poolsWithAssociatedDapps = poolsToDapps,
            actionType = PreviewType.Transfer.Pool.ActionType.Contribution
        )
    }

    private fun ExecutionSummary.extractWithdraws(allOwnedAccounts: List<Network.Account>, resources: List<Resource>) =
        accountWithdraws.entries.map { transferEntry ->
            val accountOnNetwork = allOwnedAccounts.find { it.address == transferEntry.key }

            val withdrawing = transferEntry.value.map { resourceIndicator ->
                Transferable.Withdrawing(resourceIndicator.toTransferableResource(resources))
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

suspend fun List<Pool>.resolveDApps(
    resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
): Map<Pool, DApp?> = mapNotNull { pool ->
    val dapp = pool.metadata.dAppDefinition()?.let { address ->
        resolveDAppInTransactionUseCase(address)
    }?.getOrNull()
    if (dapp == null || !dapp.second) {
        pool to null
    } else {
        pool to dapp.first
    }
}.toMap()
