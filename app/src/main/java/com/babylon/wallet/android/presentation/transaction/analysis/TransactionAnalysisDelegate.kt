package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.manifest.addLockFeeInstructionToManifest
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.TransactionConfig
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetDAppWithMetadataAndAssociatedResourcesUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import com.babylon.wallet.android.presentation.transaction.TransactionFees
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.then
import rdx.works.core.toUByteList
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
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
        state.value.request.transactionManifestData.toTransactionManifest().onSuccess {
            startAnalysis(it)
        }.onFailure { error ->
            reportFailure(error)
        }
    }

    suspend fun onFeePayerConfirmed(account: Network.Account, pendingManifest: TransactionManifest) {
        val manifestWithLockFee = pendingManifest.addLockFeeInstructionToManifest(
            addressToLockFee = account.address,
            fee = TransactionConfig.DEFAULT_LOCK_FEE.toBigDecimal()
        )

        startAnalysis(manifestWithLockFee)
    }

    private suspend fun startAnalysis(manifest: TransactionManifest) = getTransactionPreview(manifest)
        .then { preview ->
            analyzeExecution(manifest, preview)
        }.map { it.second }.resolve()

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

    private suspend fun Result<ExecutionAnalysis>.resolve() = this.onSuccess { analysis ->
        val previewType = when (val type = analysis.transactionType) {
            is TransactionType.NonConforming -> PreviewType.NonConforming
            is TransactionType.GeneralTransaction -> type.resolve(
                getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
                getDAppWithMetadataAndAssociatedResourcesUseCase = getDAppWithMetadataAndAssociatedResourcesUseCase
            )

            is TransactionType.SimpleTransfer -> type.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
            )

            is TransactionType.Transfer -> type.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
            )
        }

        state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                fees = TransactionFees(networkFee = TransactionConfig.NETWORK_FEE.toBigDecimal()),
                isLoading = false,
                previewType = previewType
            )
        }
    }.onFailure { error ->
        reportFailure(error)
    }

    private fun reportFailure(error: Throwable) {
        Timber.w(error)
        state.update {
            it.copy(
                isLoading = false,
                previewType = PreviewType.None,
                error = UiMessage.ErrorMessage.from(error)
            )
        }
    }
}
