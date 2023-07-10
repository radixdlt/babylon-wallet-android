package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionResourcesFromAnalysis
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.toUByteList
import timber.log.Timber

class TransactionAnalysisDelegate(
    private val state: MutableStateFlow<State>,
    private val getTransactionResourcesFromAnalysis: GetTransactionResourcesFromAnalysis,
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
        val analysisWithAccounts = getTransactionResourcesFromAnalysis(executionAnalysis = analysis)
    }.onFailure { error ->
        state.update {
            it.copy(
                isLoading = false,
                error = UiMessage.ErrorMessage.from(error)
            )
        }
    }

    private fun log(message: String) = Timber.tag("TransactionApproval").d(message)
}
