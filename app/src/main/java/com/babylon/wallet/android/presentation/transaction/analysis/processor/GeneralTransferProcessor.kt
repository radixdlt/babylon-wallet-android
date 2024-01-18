package com.babylon.wallet.android.presentation.transaction.analysis.processor

import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppInTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.radixdlt.ret.DetailedManifestClass
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.ResourceIndicator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import rdx.works.profile.domain.defaultDepositGuarantee
import javax.inject.Inject

class GeneralTransferProcessor @Inject constructor(
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
): PreviewTypeProcessor<DetailedManifestClass.General> {
    override suspend fun process(summary: ExecutionSummary, classification: DetailedManifestClass.General): PreviewType {
        val resources = getResourcesUseCase(addresses = summary.involvedResourceAddresses).getOrThrow()

        val badges = getTransactionBadgesUseCase(accountProofs = summary.presentedProofs)
        val dApps = summary.resolveDApps(resolveDAppInTransactionUseCase).distinctBy {
            it.first.definitionAddresses
        }
        val involvedAccountAddresses = summary.accountWithdraws.keys + summary.accountDeposits.keys
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            it.address in involvedAccountAddresses
        }

        val defaultDepositGuarantee = getProfileUseCase.defaultDepositGuarantee()

        return PreviewType.Transfer.GeneralTransfer(
            from = summary.resolveFromAccounts(resources, allAccounts),
            to = summary.resolveToAccounts(resources, allAccounts, defaultDepositGuarantee),
            badges = badges,
            dApps = dApps
        )
    }

    private suspend fun ExecutionSummary.resolveDApps(
        resolveDAppInTransactionUseCase: ResolveDAppInTransactionUseCase
    ) = coroutineScope {
        encounteredEntities.filter { it.isGlobalComponent() }
            .map { address ->
                async {
                    resolveDAppInTransactionUseCase.invoke(address.addressString())
                }
            }
            .awaitAll()
            .mapNotNull { it.getOrNull() }
    }

    private fun ExecutionSummary.resolveFromAccounts(
        allResources: List<Resource>,
        allAccounts: List<Network.Account>
    ) = accountWithdraws.map { withdrawEntry ->
        val transferables = withdrawEntry.value.map {
            it.toWithdrawingTransferableResource(
                resources = allResources,
                newlyCreatedMetadata = newEntities.metadata,
                newlyCreatedEntities = newEntities.resourceAddresses
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

    private fun ExecutionSummary.resolveToAccounts(
        allResources: List<Resource>,
        allAccounts: List<Network.Account>,
        defaultDepositGuarantees: Double
    ): List<AccountWithTransferableResources> {
        return accountDeposits.sort(allAccounts).map { depositEntry ->
            val transferables = depositEntry.value.map {
                it.toDepositingTransferableResource(
                    resources = allResources,
                    newlyCreatedMetadata = newEntities.metadata,
                    newlyCreatedEntities = newEntities.resourceAddresses,
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
}

/**
 * The account deposits order that comes from RET does not take into account the order we maintain in the app.
 * This method sorts accounts in the same order they are appearing in the dashboard.
 * TODO revisit if this can be done using Comparator.comparing { item -> allAccounts.map { it.address }.indexOf(item) }
 */
fun Map<String, List<ResourceIndicator>>.sort(allAccounts: List<Network.Account>): Map<String, List<ResourceIndicator>> {
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
