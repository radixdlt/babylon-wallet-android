package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.model.assets.Assets
import com.babylon.wallet.android.domain.model.resources.metadata.MetadataItem
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
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
import rdx.works.profile.domain.defaultDepositGuarantee

// Generic transaction resolver
suspend fun TransactionType.GeneralTransaction.resolve(
    getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    getProfileUseCase: GetProfileUseCase,
    getAccountsWithAssetsUseCase: GetAccountsWithAssetsUseCase,
    getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    resolveDAppsUseCase: ResolveDAppsUseCase
): PreviewType {
    val badges = getTransactionBadgesUseCase(accountProofs = accountProofs)
    val dApps = resolveDApps(resolveDAppsUseCase).distinctBy {
        it.dAppWithMetadata.definitionAddresses
    }

    val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
        it.address in accountWithdraws.keys || it.address in accountDeposits.keys
    }
    val allAssets = getAccountsWithAssetsUseCase(
        accounts = allAccounts,
        isRefreshing = false
    ).getOrNull().orEmpty().mapNotNull {
        it.assets
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

    val allResourcesAddresses = allAssets.map { resources ->
        val fungibleResourceAddresses = resources.fungibles.map { fungibleResource ->
            fungibleResource.resourceAddress
        }
        val nonFungibleResourceAddresses = resources.nonFungibles.map { nonFungibleResource ->
            nonFungibleResource.resourceAddress
        }
        fungibleResourceAddresses + nonFungibleResourceAddresses
    }.flatten()

    // Here we have "third party" resources that are not associated with accounts we hold
    val notOwnedResources = depositResourcesInvolvedInTransaction.filterNot { allResourcesAddresses.contains(it) }

    val thirdPartyMetadata = getResourcesMetadataUseCase(
        resourceAddresses = notOwnedResources,
        isRefreshing = false
    ).getOrNull().orEmpty()

    val defaultDepositGuarantee = getProfileUseCase.defaultDepositGuarantee()

    return PreviewType.Transfer(
        from = resolveFromAccounts(allAssets, allAccounts),
        to = resolveToAccounts(allAssets, allAccounts, thirdPartyMetadata, defaultDepositGuarantee),
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
    allResources: List<Assets>,
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
    allAssets: List<Assets>,
    allAccounts: List<Network.Account>,
    thirdPartyMetadata: Map<String, List<MetadataItem>> = emptyMap(),
    defaultDepositGuarantees: Double
) = accountDeposits.map { depositEntry ->
    val transferables = depositEntry.value.map {
        it.toDepositingTransferableResource(
            allAssets = allAssets,
            newlyCreatedMetadata = metadataOfNewlyCreatedEntities,
            newlyCreatedEntities = addressesOfNewlyCreatedEntities,
            thirdPartyMetadata = thirdPartyMetadata,
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
