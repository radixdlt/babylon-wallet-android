package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.ResourceTracker
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.defaultDepositGuarantee

// Generic transaction resolver
suspend fun TransactionType.GeneralTransaction.resolve(
    resources: List<Resource>,
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    getProfileUseCase: GetProfileUseCase,
    resolveDAppsUseCase: ResolveDAppsUseCase
): PreviewType {
    val badges = getTransactionBadgesUseCase(accountProofs = accountProofs)
    val dApps = resolveDApps(resolveDAppsUseCase).distinctBy {
        it.dApp.definitionAddresses
    }

    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address in accountWithdraws.keys || it.address in accountDeposits.keys
    }

    val defaultDepositGuarantee = getProfileUseCase.defaultDepositGuarantee()

    return PreviewType.Transfer(
        from = resolveFromAccounts(resources, allAccounts),
        to = resolveToAccounts(resources, allAccounts, defaultDepositGuarantee),
        badges = badges,
        dApps = dApps
    )
}

private suspend fun TransactionType.GeneralTransaction.resolveDApps(
    resolveDAppsUseCase: ResolveDAppsUseCase
) = coroutineScope {
    addressesInManifest[EntityType.GLOBAL_GENERIC_COMPONENT].orEmpty()
        .map { address ->
            async {
                resolveDAppsUseCase.invoke(address.addressString())
            }
        }
        .awaitAll()
        .mapNotNull { it.getOrNull() }
}

private fun TransactionType.GeneralTransaction.resolveFromAccounts(
    allResources: List<Resource>,
    allAccounts: List<Network.Account>
) = accountWithdraws.map { withdrawEntry ->
    val transferables = withdrawEntry.value.map {
        it.toWithdrawingTransferableResource(
            resources = allResources,
            newlyCreatedMetadata = metadataOfNewlyCreatedEntities,
            newlyCreatedEntities = addressesOfNewlyCreatedEntities
        )
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

/**
 * The account deposits order that comes from RET does not take into account the order we maintain in the app.
 * This method sorts accounts in the same order they are appearing in the dashboard.
 * TODO revisit if this can be done using Comparator.comparing { item -> allAccounts.map { it.address }.indexOf(item) }
 */
fun Map<String, List<ResourceTracker>>.sort(allAccounts: List<Network.Account>): Map<String, List<ResourceTracker>> {
    val allAccountsAddresses = allAccounts.map { it.address }

    // Only account deposits that we own
    val ownedAccountDeposits = this.toList().filter {
        allAccountsAddresses.indexOf(it.first) != -1
    }

    // Sorted owned accounts deposits according to the all accounts order
    val ownedAccountDepositsSorted = ownedAccountDeposits.sortedBy {
        allAccountsAddresses.indexOf(it.first)
    }.toMap()

    // The rest of account deposits (ones we do not own)
    val thirdPartyAccountDeposits = this.minus(ownedAccountDepositsSorted.keys)

    return ownedAccountDepositsSorted.plus(thirdPartyAccountDeposits)
}

private fun TransactionType.GeneralTransaction.resolveToAccounts(
    allResources: List<Resource>,
    allAccounts: List<Network.Account>,
    defaultDepositGuarantees: Double
): List<AccountWithTransferableResources> {
    return accountDeposits.sort(allAccounts).map { depositEntry ->
        val transferables = depositEntry.value.map {
            it.toDepositingTransferableResource(
                resources = allResources,
                newlyCreatedMetadata = metadataOfNewlyCreatedEntities,
                newlyCreatedEntities = addressesOfNewlyCreatedEntities,
                defaultDepositGuarantees = defaultDepositGuarantees
            )
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
}
