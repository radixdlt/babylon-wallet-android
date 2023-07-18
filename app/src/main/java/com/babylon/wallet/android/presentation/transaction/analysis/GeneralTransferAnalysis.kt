package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork

// Generic transaction resolver
suspend fun TransactionType.GeneralTransaction.resolve(
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase
): PreviewType {
    val badges = getTransactionBadgesUseCase(accountProofs = accountProofs)
    val dApps = resolveDApps(getDAppWithMetadataAndAssociatedResourcesUseCase)

    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address in accountWithdraws.keys || it.address in accountDeposits.keys
    }
    val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
        it.resources
    }

    return PreviewType.Transaction(
        from = resolveFromAccounts(allResources, allAccounts),
        to = resolveToAccounts(allResources, allAccounts),
        badges = badges,
        dApps = dApps
    )
}

private suspend fun TransactionType.GeneralTransaction.resolveDApps(
    getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase
) = coroutineScope {
    addressesInManifest[EntityType.GLOBAL_GENERIC_COMPONENT].orEmpty()
        .map { address ->
            async {
                getDAppWithMetadataAndAssociatedResourcesUseCase(
                    definitionAddress = address.addressString(),
                    needMostRecentData = true
                )
            }
        }
        .awaitAll()
        .mapNotNull { it.value() }
}

private fun TransactionType.GeneralTransaction.resolveFromAccounts(
    allResources: List<Resources>,
    allAccounts: List<Network.Account>
) = accountWithdraws.map { withdrawEntry ->
    val transferables = withdrawEntry.value.map {
        it.toWithdrawingTransferableResource(allResources, metadataOfNewlyCreatedEntities)
    }

    val ownedAccount = allAccounts.find { it.address == withdrawEntry.key }
    if (ownedAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = transferables
        )
    } else {
        AccountWithTransferableResources.Other(
            address = withdrawEntry.key,
            resources = transferables
        )
    }
}

private fun TransactionType.GeneralTransaction.resolveToAccounts(
    allResources: List<Resources>,
    allAccounts: List<Network.Account>
) = accountDeposits.map { depositEntry ->
    val transferables = depositEntry.value.map {
        it.toDepositingTransferableResource(allResources, metadataOfNewlyCreatedEntities)
    }

    val ownedAccount = allAccounts.find { it.address == depositEntry.key }
    if (ownedAccount != null) {
        AccountWithTransferableResources.Owned(
            account = ownedAccount,
            resources = transferables
        )
    } else {
        AccountWithTransferableResources.Other(
            address = depositEntry.key,
            resources = transferables
        )
    }
}
