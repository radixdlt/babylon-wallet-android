package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.Assets
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.TransactionType
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun TransactionType.Transfer.resolve(
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithAssetsUseCase: GetAccountsWithAssetsUseCase
): PreviewType {
    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address == from.addressString() || it.address in transfers.keys
    }
    val allAssets = getAccountsWithAssetsUseCase(
        accounts = allAccounts,
        isRefreshing = false
    ).value().orEmpty().mapNotNull {
        it.assets
    }

    val to = extractDeposits(allAccounts, allAssets)

    // Taking the accumulated values of fungibles and non fungibles and add them to one from account
    val fromAccount = allAccounts.find { it.address == from.addressString() }
    val allTransferringResources = to.map { depositing ->
        depositing.resources.map { it.transferable }
    }.flatten().groupBy { it.resourceAddress }

    val withdrawingResources = extractWithdraws(allTransferringResources)

    val from = if (fromAccount == null) {
        AccountWithTransferableResources.Other(
            address = from.addressString(),
            resources = withdrawingResources
        )
    } else {
        AccountWithTransferableResources.Owned(
            account = fromAccount,
            resources = withdrawingResources
        )
    }

    return PreviewType.Transfer(
        from = listOf(from),
        to = to
    )
}

private fun TransactionType.Transfer.extractDeposits(
    allAccounts: List<Network.Account>,
    allAssets: List<Assets>
) = transfers.entries.map { transferEntry ->
    val accountOnNetwork = allAccounts.find { it.address == transferEntry.key }

    val resources = transferEntry.value.map { transferringEntry ->
        transferringEntry.value.toTransferableResource(transferringEntry.key, allAssets)
    }

    accountOnNetwork?.let { account ->
        AccountWithTransferableResources.Owned(
            account = account,
            resources = resources.map { Transferable.Depositing(it) }
        )
    } ?: AccountWithTransferableResources.Other(
        address = transferEntry.key,
        resources = resources.map { Transferable.Depositing(it) }
    )
}

private fun extractWithdraws(allTransferringResources: Map<String, List<TransferableResource>>) =
    allTransferringResources.mapNotNull { entry ->
        val fungibleTransferrables = entry.value.filterIsInstance<TransferableResource.Amount>()
        val nonFungibleTransferrables = entry.value.filterIsInstance<TransferableResource.NFTs>()

        if (fungibleTransferrables.isNotEmpty()) {
            val transferrable = fungibleTransferrables.reduce { transferable, value ->
                transferable.copy(amount = transferable.amount + value.amount)
            }
            Transferable.Withdrawing(transferrable)
        } else if (nonFungibleTransferrables.isNotEmpty()) {
            val nonFungibleTransferrable = nonFungibleTransferrables.reduce { nonFungibleTransferrable, value ->
                nonFungibleTransferrable.copy(
                    resource = nonFungibleTransferrable.resource.copy(
                        amount = nonFungibleTransferrable.resource.amount + value.resource.amount,
                        items = nonFungibleTransferrable.resource.items + value.resource.items
                    )
                )
            }
            Transferable.Withdrawing(nonFungibleTransferrable)
        } else {
            null
        }
    }
