package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber

@Suppress("LongParameterList")
class TransactionAnalysisDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val resolveDAppsUseCase: ResolveDAppsUseCase,
    private val transactionClient: TransactionClient,
    private val logger: Timber.Tree
) {

    suspend fun analyse() {
        state.value.request.transactionManifestData.toTransactionManifest().onSuccess {
            startAnalysis(it)
        }.onFailure { error ->
            reportFailure(error)
        }
    }

    private suspend fun startAnalysis(manifest: TransactionManifest) = getTransactionPreview(manifest)
        .then { preview ->
            transactionClient.analyzeExecution(manifest, preview)
        }.resolve(manifest)

    private suspend fun getTransactionPreview(manifest: TransactionManifest) = transactionClient.getTransactionPreview(
        manifest = manifest,
        ephemeralNotaryPrivateKey = state.value.ephemeralNotaryPrivateKey
    )

    private suspend fun Result<ExecutionAnalysis>.resolve(manifest: TransactionManifest) = this.onSuccess { analysis ->
        val transactionTypes = analysis.transactionTypes
        val previewType = if (transactionTypes.isEmpty()) {
            PreviewType.NonConforming
        } else {
            when (val type = analysis.transactionTypes[0]) {
                is TransactionType.GeneralTransaction -> type.resolve(
                    getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                    getProfileUseCase = getProfileUseCase,
                    getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
                    getResourcesMetadataUseCase = getResourcesMetadataUseCase,
                    resolveDAppsUseCase = resolveDAppsUseCase
                )

                is TransactionType.SimpleTransfer -> type.resolve(
                    getProfileUseCase = getProfileUseCase,
                    getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
                )

                is TransactionType.Transfer -> type.resolve(
                    getProfileUseCase = getProfileUseCase,
                    getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
                )

                else -> { PreviewType.NonConforming }
            }
        }

        val transactionFees = TransactionFees(
            nonContingentFeeLock = analysis.feeLocks.lock.asStr().toBigDecimal(),
            networkExecution = analysis.feeSummary.executionCost.asStr().toBigDecimal(),
            networkFinalization = analysis.feeSummary.finalizationCost.asStr().toBigDecimal(),
            networkStorage = analysis.feeSummary.storageExpansionCost.asStr().toBigDecimal(),
            royalties = analysis.feeSummary.royaltyCost.asStr().toBigDecimal(),
        )

        transactionClient.findFeePayerInManifest(
            manifest = manifest,
            lockFee = transactionFees.defaultTransactionFee
        ).onSuccess { feePayerResult ->
            val candidateXrdBalance = feePayerResult.candidateXrdBalance()

            state.update {
                it.copy(
                    isRawManifestVisible = previewType == PreviewType.NonConforming,
                    transactionFees = transactionFees,
                    isLoading = false,
                    previewType = previewType,
                    feePayerSearchResult = feePayerResult.copy(
                        insufficientBalanceToPayTheFee = candidateXrdBalance < transactionFees.defaultTransactionFee
                    )
                )
            }
        }
    }.onFailure { error ->
        reportFailure(error)
    }

    private fun reportFailure(error: Throwable) {
        logger.w(error)

        state.update {
            it.copy(
                isLoading = false,
                previewType = PreviewType.None,
                error = UiMessage.ErrorMessage.from(error)
            )
        }
    }
}
