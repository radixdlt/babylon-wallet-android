package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.common.value
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.TransferableResource
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.AccountWithTransferableResources
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.toUByteList
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.accountsOnCurrentNetwork
import timber.log.Timber

class TransactionAnalysisDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val transactionClient: TransactionClient
) {

    suspend fun analyse() {
        val manifest = state.value.request.transactionManifestData.toTransactionManifest()
        getTransactionPreview(manifest = manifest).onSuccess {
            analyzeExecution(
                manifest = manifest,
                preview = it
            )
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

    private suspend fun analyzeExecution(
        manifest: TransactionManifest,
        preview: TransactionPreviewResponse
    ) = runCatching {
        manifest.analyzeExecution(transactionReceipt = preview.encodedReceipt.decodeHex().toUByteList())
    }.onSuccess { analysis ->
        val previewType = when (val type = analysis.transactionType) {
            is TransactionType.NonConforming -> PreviewType.NonConforming
            is TransactionType.GeneralTransaction -> PreviewType.NonConforming
            is TransactionType.SimpleTransfer -> resolve(analysis, type)
            is TransactionType.Transfer -> resolve(analysis, type)
        }

        state.update { it.copy(previewType = previewType) }
    }.onFailure { error ->
        state.update {
            it.copy(
                isLoading = false,
                error = UiMessage.ErrorMessage.from(error)
            )
        }
    }

    private suspend fun resolve(
        executionAnalysis: ExecutionAnalysis,
        simpleTransfer: TransactionType.SimpleTransfer
    ): PreviewType {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
            it.resources
        }

        val transferableResource = simpleTransfer.transferred.toTransferableResource(allResources = allResources)
        val fromAccount = allAccounts.find { it.address == simpleTransfer.from.addressString() }?.let {
            AccountWithTransferableResources.Owned(
                account = it,
                resources = listOf(Transferable.Withdrawing(transferableResource))
            )
        } ?: AccountWithTransferableResources.Other(address = simpleTransfer.from.addressString(), resources = listOf())

        val toAccount = allAccounts.find { it.address == simpleTransfer.to.addressString() }?.let {
            AccountWithTransferableResources.Owned(
                account = it,
                resources = listOf(Transferable.Depositing(transferableResource))
            )
        } ?: AccountWithTransferableResources.Other(address = simpleTransfer.to.addressString(), resources = listOf())

        return PreviewType.Transaction(from = listOf(fromAccount), to = listOf(toAccount))
    }

    private suspend fun resolve(
        executionAnalysis: ExecutionAnalysis,
        simpleTransfer: TransactionType.Transfer
    ): PreviewType {
        val allAccounts = getProfileUseCase.accountsOnCurrentNetwork()
        val allResources = getAccountsWithResourcesUseCase(accounts = allAccounts, isRefreshing = false).value().orEmpty().mapNotNull {
            it.resources
        }

        val to = simpleTransfer.transfers.entries.map { transfer ->
            val accountOnNetwork = allAccounts.find { it.address == transfer.key }

            val resources = transfer.value.map { transferringEntry ->
                transferringEntry.value.toTransferableResource(transferringEntry.key, allResources)
            }

            accountOnNetwork?.let { account ->
                AccountWithTransferableResources.Owned(
                    account = account,
                    resources = resources.map { Transferable.Depositing(it) }
                )
            } ?: AccountWithTransferableResources.Other(
                address = transfer.key,
                resources = resources.map { Transferable.Depositing(it) }
            )
        }

        // Withdrawing the same items as we deposit
        val from = to.map { depositingAccount ->
            when (depositingAccount) {
                is AccountWithTransferableResources.Other -> AccountWithTransferableResources.Other(
                    address = depositingAccount.address,
                    resources = depositingAccount.resources.map { Transferable.Withdrawing(it.transferable) }
                )
                is AccountWithTransferableResources.Owned -> AccountWithTransferableResources.Owned(
                    account = depositingAccount.account,
                    resources = depositingAccount.resources.map { Transferable.Withdrawing(it.transferable) }
                )
            }
        }

        return PreviewType.Transaction(
            from = from,
            to = to
        )
    }

    private fun log(message: String) = Timber.tag("TransactionApproval").d(message)
}
