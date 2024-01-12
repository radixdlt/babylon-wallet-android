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
        val deposits = depositsPerAddress.value.map { deposit ->
            val resourceAddress = deposit.resourceAddress
            val contribution = poolContributions.find { it.poolUnitsResourceAddress.addressString() == resourceAddress }
                ?: error("No contribution found")
            val pool = involvedPools.find { it.address == contribution.poolAddress.addressString() }
                ?: error("No pool found")
            val poolResource = resources.find { it.resourceAddress == pool.metadata.poolUnit() } as? Resource.FungibleResource
                ?: error("No pool resource found")
            val contributedResourceAddresses = contribution.contributedResources.keys
            Transferable.Depositing(
                transferable = TransferableResource.Pool(
                    PoolUnit(
                        stake = poolResource,
                        pool = pool
                    ),
                    contributedResourceAddresses.associateWith { contributedResourceAddress ->
                        val contributionValue = contribution.contributedResources[contributedResourceAddress]
                            ?: error("No contribution value found")
                        contributionValue.asStr().toBigDecimal()
                    },
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
