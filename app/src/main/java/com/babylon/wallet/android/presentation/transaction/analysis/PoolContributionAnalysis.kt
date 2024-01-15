package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber

suspend fun DetailedManifestClass.PoolContribution.resolve(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedPools: List<Pool>
): PreviewType {
    val accountsWithdrawnFrom = executionSummary.accountWithdraws.keys
    val ownedAccountsWithdrawnFrom = getProfileUseCase.accountsOnCurrentNetwork().filter {
        accountsWithdrawnFrom.contains(it.address)
    }
    val from = executionSummary.extractWithdraws(ownedAccountsWithdrawnFrom, resources)
    val to = executionSummary.accountDeposits.map { depositsPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(depositsPerAddress.key) ?: error("No account found")
        depositsPerAddress.value.forEach {
            Timber.d("depositing: ${it.resourceAddress}")
        }
        val deposits = depositsPerAddress.value.mapNotNull { deposit ->
            val resourceAddress = deposit.resourceAddress
            val contributions = poolContributions.filter { it.poolUnitsResourceAddress.addressString() == resourceAddress }
            if (contributions.isEmpty()) {
                null
            } else {
                val pool = involvedPools.find { it.address == contributions.first().poolAddress.addressString() }
                    ?: error("No pool found")
                val poolResource = resources.find { it.resourceAddress == pool.metadata.poolUnit() } as? Resource.FungibleResource
                    ?: error("No pool resource found")
                val contributedResourceAddresses = contributions.first().contributedResources.keys
                Transferable.Depositing(
                    transferable = TransferableResource.PoolUnitAmount(
                        amount = contributions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                        PoolUnit(
                            stake = poolResource,
                            pool = pool
                        ),
                        contributedResourceAddresses.associateWith { contributedResourceAddress ->
                            contributions.mapNotNull { it.contributedResources[contributedResourceAddress]?.asStr()?.toBigDecimal() }
                                .sumOf { it }
                        },
                    )
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
        pools = involvedPools,
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
