package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.Resources
import com.babylon.wallet.android.domain.model.metadata.MetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
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

// Generic transaction resolver
suspend fun TransactionType.GeneralTransaction.resolve(
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    resolveDAppsUseCase: ResolveDAppsUseCase
): PreviewType {
    val badges = getTransactionBadgesUseCase(accountProofs = accountProofs)
    val dApps = resolveDApps(resolveDAppsUseCase)

    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address in accountWithdraws.keys || it.address in accountDeposits.keys
    }
    val allResources = getAccountsWithResourcesUseCase(
        accounts = allAccounts,
        isRefreshing = false
    ).value().orEmpty().mapNotNull {
        it.resources
    }

    val depositResourcesInvolvedInTransaction = this.accountDeposits.values.map {
        it.map { resourceTracker ->
            when (resourceTracker) {
                is ResourceTracker.Fungible -> {
                    resourceTracker.resourceAddress.addressString()
                }
                is ResourceTracker.NonFungible -> {
                    resourceTracker.resourceAddress.addressString()
                }
            }
        }
    }.flatten()

    val allResourcesAddresses = allResources.map { resources ->
        val fungibleResourceAddresses = resources.fungibleResources.map { fungibleResource ->
            fungibleResource.resourceAddress
        }
        val nonFungibleResourceAddresses = resources.nonFungibleResources.map { nonFungibleResource ->
            nonFungibleResource.resourceAddress
        }
        fungibleResourceAddresses + nonFungibleResourceAddresses
    }.flatten()

    // Here we have "third party" resources that are not associated with accounts we hold
    val notOwnedResources = depositResourcesInvolvedInTransaction.filterNot { allResourcesAddresses.contains(it) }

    val thirdPartyMetadata = getResourcesMetadataUseCase
        .invoke(resourceAddresses = notOwnedResources, isRefreshing = false).value().orEmpty()

    return PreviewType.Transaction(
        from = resolveFromAccounts(allResources, allAccounts),
        to = resolveToAccounts(allResources, allAccounts, thirdPartyMetadata),
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
    allResources: List<Resources>,
    allAccounts: List<Network.Account>
) = accountWithdraws.map { withdrawEntry ->
    val transferables = withdrawEntry.value.map {
        it.toWithdrawingTransferableResource(
            allResources = allResources,
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

private fun TransactionType.GeneralTransaction.resolveToAccounts(
    allResources: List<Resources>,
    allAccounts: List<Network.Account>,
    thirdPartyMetadata: Map<String, List<MetadataItem>> = emptyMap()
) = accountDeposits.map { depositEntry ->
    val transferables = depositEntry.value.map {
        it.toDepositingTransferableResource(
            allResources = allResources,
            newlyCreatedMetadata = metadataOfNewlyCreatedEntities,
            newlyCreatedEntities = addressesOfNewlyCreatedEntities,
            thirdPartyMetadata = thirdPartyMetadata
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
