package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.TransactionType
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

suspend fun TransactionType.SimpleTransfer.resolve(
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase
): PreviewType {
    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address == from.addressString() || it.address == to.addressString()
    }
    val allResources = getAccountsWithResourcesUseCase(
        accounts = allAccounts,
        isRefreshing = false
    ).value().orEmpty().mapNotNull {
        it.resources
    }

    val transferableResource = transferred.toTransferableResource(allResources = allResources)
    val ownedFromAccount = allAccounts.find { it.address == from.addressString() }
    val fromAccount = if (ownedFromAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedFromAccount,
            resources = listOf(Transferable.Withdrawing(transferableResource))
        )
    } else {
        AccountWithTransferableResources.Other(
            address = from.addressString(),
            resources = listOf(Transferable.Withdrawing(transferableResource))
        )
    }

    val ownedToAccount = allAccounts.find { it.address == to.addressString() }
    val toAccount = if (ownedToAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedToAccount,
            resources = listOf(Transferable.Depositing(transferableResource))
        )
    } else {
        AccountWithTransferableResources.Other(
            address = to.addressString(),
            resources = listOf(Transferable.Depositing(transferableResource))
        )
    }

    return PreviewType.Transfer(from = listOf(fromAccount), to = listOf(toAccount))
}
