package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.model.assets.PoolUnit
import com.babylon.wallet.android.domain.model.resources.Pool
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.model.resources.metadata.poolUnit
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountOnCurrentNetwork
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun DetailedManifestClass.PoolRedemption.resolve(
    executionSummary: ExecutionSummary,
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
    involvedPools: List<Pool>,
    resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
): PreviewType.Transfer.Pool {
    val poolsToDapps = involvedPools.resolveDApps(resolveDAppInTransactionUseCase)
    val accountsWithdrawnFrom = executionSummary.accountDeposits.keys
    val ownedAccountsWithdrawnFrom = getProfileUseCase.accountsOnCurrentNetwork().filter {
        accountsWithdrawnFrom.contains(it.address)
    }
    val to = executionSummary.extractDeposits(ownedAccountsWithdrawnFrom, resources)
    val from = executionSummary.accountWithdraws.map { withdrawsPerAddress ->
        val ownedAccount = getProfileUseCase.accountOnCurrentNetwork(withdrawsPerAddress.key) ?: error("No account found")
        val deposits = withdrawsPerAddress.value.map { withdraw ->
            val resourceAddress = withdraw.resourceAddress
            val redemptions = poolRedemptions.filter {
                it.poolUnitsResourceAddress.addressString() == resourceAddress
            }
            val pool = involvedPools.find { it.address == redemptions.first().poolAddress.addressString() }
                ?: error("No pool found")
            val poolResource = resources.find { it.resourceAddress == pool.metadata.poolUnit() } as? Resource.FungibleResource
                ?: error("No pool resource found")
            val redemptionResourceAddresses = redemptions.first().redeemedResources.keys
            Transferable.Withdrawing(
                transferable = TransferableResource.PoolUnitAmount(
                    amount = redemptions.map { it.poolUnitsAmount.asStr().toBigDecimal() }.sumOf { it },
                    PoolUnit(
                        stake = poolResource,
                        pool = pool
                    ),
                    redemptionResourceAddresses.associateWith { contributedResourceAddress ->
                        redemptions.mapNotNull { it.redeemedResources[contributedResourceAddress]?.asStr()?.toBigDecimal() }
                            .sumOf { it }
                    },
                    associatedDapp = poolsToDapps[pool]
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
        actionType = PreviewType.Transfer.Pool.ActionType.Redemption,
        poolsWithAssociatedDapps = poolsToDapps
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
