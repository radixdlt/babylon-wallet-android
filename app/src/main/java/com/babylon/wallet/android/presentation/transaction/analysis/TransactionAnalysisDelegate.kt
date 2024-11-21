package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummarizedManifest
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.analysis.summary.execution.ExecutionSummaryToPreviewTypeAnalyser
import com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest.ManifestSummaryToPreviewTypeAnalyser
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.TransactionToReview
import kotlinx.coroutines.flow.update
import rdx.works.core.sargon.formatted
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val executionSummaryToPreviewTypeAnalyser: ExecutionSummaryToPreviewTypeAnalyser,
    private val manifestSummaryToPreviewTypeAnalyser: ManifestSummaryToPreviewTypeAnalyser,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val getProfileUseCase: GetProfileUseCase
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    /**
     * Runs analysis on each transaction type received. Resolves the preview type and returns the signers involved.
     */
    suspend fun analyse(): Result<Analysis> = when (val kind = data.value.request.kind) {
        is TransactionRequest.Kind.PreAuthorized -> analysePreAuthTransaction(
            preAuthToReview = kind.preAuthToReview
        )

        is TransactionRequest.Kind.Regular -> analyseTransaction(
            transactionToReview = kind.transactionToReview
        )
    }.onSuccess { analysis ->
        cacheNewlyCreatedResources(previewType = analysis.previewType)
    }.onSuccess { analysis ->
        _state.update {
            it.copy(
                isRawManifestVisible = analysis.previewType == PreviewType.NonConforming,
                showRawTransactionWarning = analysis.previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = analysis.previewType,
            )
        }
    }.onFailure { error -> onError(error) }

    private suspend fun analyseTransaction(
        transactionToReview: TransactionToReview
    ): Result<Analysis> = runCatching {
        val summary = Summary.FromExecution(
            manifest = SummarizedManifest.Transaction(transactionToReview.transactionManifest),
            summary = transactionToReview.executionSummary
        )

        val profile = getProfileUseCase()
        val previewType = executionSummaryToPreviewTypeAnalyser.analyze(summary)

        Analysis(
            previewType = previewType,
            summary = summary,
            profile = profile
        )
    }

    private suspend fun analysePreAuthTransaction(
        preAuthToReview: PreAuthToReview
    ): Result<Analysis> = runCatching {
        val profile = getProfileUseCase()

        when (preAuthToReview) {
            is PreAuthToReview.Enclosed -> {
                val summary = Summary.FromExecution(
                    manifest = SummarizedManifest.Subintent(preAuthToReview.v1.manifest),
                    summary = preAuthToReview.v1.summary
                )
                val previewType = executionSummaryToPreviewTypeAnalyser.analyze(summary)

                Analysis(
                    previewType = previewType,
                    summary = summary,
                    profile = profile
                )
            }

            is PreAuthToReview.Open -> {
                val summary = Summary.FromStaticAnalysis(
                    manifest = SummarizedManifest.Subintent(preAuthToReview.v1.manifest),
                    summary = preAuthToReview.v1.summary
                )
                val previewType = manifestSummaryToPreviewTypeAnalyser.analyze(summary = summary)

                Analysis(
                    previewType = previewType,
                    summary = summary,
                    profile = profile
                )
            }
        }
    }

    private suspend fun cacheNewlyCreatedResources(previewType: PreviewType) {
        if (previewType is PreviewType.Transaction) {
            val newlyCreatedResources = previewType.newlyCreatedResources
            if (newlyCreatedResources.isNotEmpty()) {
                cacheNewlyCreatedEntitiesUseCase.forResources(newlyCreatedResources)
            }
            val newlyCreatedNFTItemsForExistingResources = previewType.newlyCreatedNFTs
            if (newlyCreatedNFTItemsForExistingResources.isNotEmpty()) {
                cacheNewlyCreatedEntitiesUseCase.forNFTs(newlyCreatedNFTItemsForExistingResources)
            }
        }
    }

    private fun onError(error: Throwable) {
        logger.w(error)

        when (error) {
            is CommonException.ReservedInstructionsNotAllowedInManifest -> {
                _state.update {
                    it.copy(
                        error = TransactionErrorMessage(RadixWalletException.DappRequestException.UnacceptableManifest),
                        isRawManifestVisible = false,
                        showRawTransactionWarning = false,
                        isLoading = false,
                        previewType = PreviewType.UnacceptableManifest
                    )
                }
            }

            is RadixWalletException.ResourceCouldNotBeResolvedInTransaction -> {
                Timber.w(
                    "Resource address ${
                        error.address.formatted(
                            AddressFormat.RAW
                        )
                    } neither on ledger nor as newly created entity. Defaulting to Non Conforming view."
                )

                _state.update {
                    it.copy(
                        isRawManifestVisible = true,
                        showRawTransactionWarning = true,
                        isLoading = false,
                        previewType = PreviewType.NonConforming
                    )
                }
            }

            is CommonException.OneOfReceivingAccountsDoesNotAllowDeposits -> {
                reportFailure(RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits)
            }

            else -> {
                reportFailure(RadixWalletException.DappRequestException.PreviewError(error))
            }
        }
    }

    private fun reportFailure(error: Throwable) {
        logger.w(error)

        _state.update {
            it.copy(
                isLoading = false,
                previewType = PreviewType.None,
                error = TransactionErrorMessage(error)
            )
        }
    }
}
