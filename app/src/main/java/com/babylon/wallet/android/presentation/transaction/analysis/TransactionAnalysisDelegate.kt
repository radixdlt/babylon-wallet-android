package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.GuaranteeType
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.AccountWithPredictedGuarantee
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.babylon.wallet.android.presentation.transaction.TransactionFees
import com.radixdlt.ret.EntityType
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.then
import rdx.works.core.toUByteList
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber

class TransactionAnalysisDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val getDAppWithMetadataAndAssociatedResourcesUseCase: GetDAppWithMetadataAndAssociatedResourcesUseCase,
    private val transactionClient: TransactionClient
) {

    suspend fun analyse() {
        val manifest = state.value.request.transactionManifestData.toTransactionManifest()
        getTransactionPreview(manifest = manifest).then { preview ->
            analyzeExecution(manifest = manifest, preview = preview)
        }.onSuccess { analysis ->
            val previewType = when (val type = analysis.transactionType) {
                is TransactionType.NonConforming -> PreviewType.NonConforming
                is TransactionType.GeneralTransaction -> resolve(type)
                is TransactionType.SimpleTransfer -> resolve(type)
                is TransactionType.Transfer -> resolve(type)
            }

            state.update {
                it.copy(
                    fees = TransactionFees(networkFee = TransactionConfig.NETWORK_FEE.toBigDecimal()),
                    isLoading = false,
                    previewType = previewType
                )
            }
        }.onFailure { error ->
            state.update {
                it.copy(
                    isLoading = false,
                    error = UiMessage.ErrorMessage.from(error)
                )
            }
        }
    }

    private suspend fun getTransactionPreview(manifest: TransactionManifest) = transactionClient.getTransactionPreview(
        manifest = manifest,
        ephemeralNotaryPrivateKey = state.value.ephemeralNotaryPrivateKey
    ).onSuccess { preview ->
        preview.receipt.feeSummary.let {
            // TODO update network fee, will be done properly when backend implements this
            // val costUnitPrice = feeSummary.cost_unit_price.toBigDecimal()
            // val costUnitsConsumed = feeSummary.cost_units_consumed.toBigDecimal()
        }
    }

    private fun analyzeExecution(
        manifest: TransactionManifest,
        preview: TransactionPreviewResponse
    ) = runCatching {
        manifest.analyzeExecution(transactionReceipt = preview.encodedReceipt.decodeHex().toUByteList())
    }

    // Generic transaction resolver
    private suspend fun resolve(
        transfer: TransactionType.GeneralTransaction
    ): PreviewType {
        val badges = getTransactionBadgesUseCase(accountProofs = transfer.accountProofs)
        val dApps = coroutineScope {
            transfer.addressesInManifest[EntityType.GLOBAL_GENERIC_COMPONENT].orEmpty()
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

        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            it.address in transfer.accountWithdraws.keys || it.address in transfer.accountDeposits.keys
        }
        val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
            it.resources
        }

        val fromAccounts = transfer.accountWithdraws.map { withdrawEntry ->
            val transferables = withdrawEntry.value.map {
                Transferable.Withdrawing(transferable = it.toTransferableResource(allResources, transfer.metadataOfNewlyCreatedEntities))
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

        val toAccounts = transfer.accountDeposits.map { depositEntry ->
            val transferables = depositEntry.value.map {
                it.toDepositingTransferableResource(allResources, transfer.metadataOfNewlyCreatedEntities)
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

        return PreviewType.Transaction(
            from = fromAccounts,
            to = toAccounts,
            badges = badges,
            dApps = dApps
        )
    }

    // Simple transfer resolver
    private suspend fun resolve(
        transfer: TransactionType.SimpleTransfer
    ): PreviewType {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            it.address == transfer.from.addressString() || it.address == transfer.to.addressString()
        }
        val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
            it.resources
        }

        val transferableResource = transfer.transferred.toTransferableResource(allResources = allResources)
        val ownedFromAccount = allAccounts.find { it.address == transfer.from.addressString() }
        val fromAccount = if (ownedFromAccount != null) {
            AccountWithTransferableResources.Owned(
                account = ownedFromAccount,
                resources = listOf(Transferable.Withdrawing(transferableResource))
            )
        } else {
            AccountWithTransferableResources.Other(
                address = transfer.from.addressString(),
                resources = listOf(Transferable.Withdrawing(transferableResource))
            )
        }

        val ownedToAccount = allAccounts.find { it.address == transfer.to.addressString() }
        val toAccount = if (ownedToAccount != null) {
            AccountWithTransferableResources.Owned(
                account = ownedToAccount,
                resources = listOf(Transferable.Depositing(transferableResource))
            )
        } else {
            AccountWithTransferableResources.Other(
                address = transfer.to.addressString(),
                resources = listOf(Transferable.Depositing(transferableResource))
            )
        }

        return PreviewType.Transaction(from = listOf(fromAccount), to = listOf(toAccount))
    }

    // Transfer resolver
    private suspend fun resolve(
        transfer: TransactionType.Transfer
    ): PreviewType {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork().filter {
            it.address == transfer.from.addressString() || it.address in transfer.transfers.keys
        }
        val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
            it.resources
        }

        val to = transfer.transfers.entries.map { transferEntry ->
            val accountOnNetwork = allAccounts.find { it.address == transferEntry.key }

            val resources = transferEntry.value.map { transferringEntry ->
                transferringEntry.value.toTransferableResource(transferringEntry.key, allResources)
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

        // Taking the accumulated values of fungibles and non fungibles and add them to one from account
        val fromAccount = allAccounts.find { it.address == transfer.from.addressString() }
        val allTransferringResources = to.map { depositing ->
            depositing.resources.map { it.transferable }
        }.flatten().groupBy { it.resourceAddress }

        val withdrawingResources = allTransferringResources.mapNotNull { entry ->
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

        val from = if (fromAccount == null) {
            AccountWithTransferableResources.Other(
                address = transfer.from.addressString(),
                resources = withdrawingResources
            )
        } else {
            AccountWithTransferableResources.Owned(
                account = fromAccount,
                resources = withdrawingResources
            )
        }

        return PreviewType.Transaction(
            from = listOf(from),
            to = to
        )
    }

    private fun log(message: String) = Timber.tag("TransactionApproval").d(message)
}
