package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.ExecutionSummary
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun ExecutionSummary.resolveTransfer(
    getProfileUseCase: GetProfileUseCase,
    resources: List<Resource>,
): PreviewType {
    val allInvolvedAddresses = accountDeposits.keys + accountWithdraws.keys
    val allOwnedAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        allInvolvedAddresses.contains(it.address)
    }

    val to = extractDeposits(allOwnedAccounts, resources)
    val from = extractWithdraws(allOwnedAccounts, resources)

    return PreviewType.Transfer(
        from = from,
        to = to
    )
}

private fun ExecutionSummary.extractDeposits(
    allOwnedAccounts: List<Network.Account>,
    resources: List<Resource>
) = accountDeposits.entries.map { transferEntry ->
    val accountOnNetwork = allOwnedAccounts.find { it.address == transferEntry.key }

    val depositing = transferEntry.value.map { resourceIndicator ->
        Transferable.Depositing(resourceIndicator.toTransferableResource(resources))
    }
    accountOnNetwork?.let { account ->
        AccountWithTransferableResources.Owned(
            account = account,
            resources = depositing
        )
    } ?: AccountWithTransferableResources.Other(
        address = transferEntry.key,
        resources = depositing
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
