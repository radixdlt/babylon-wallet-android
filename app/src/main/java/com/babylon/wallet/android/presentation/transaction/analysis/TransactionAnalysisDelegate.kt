package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ExecutionSummaryAnalyser
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummarizedManifest
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest.ManifestSummaryAnalyser
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.secureRandom
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.flow.update
import rdx.works.core.sargon.formatted
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val executionSummaryAnalyser: ExecutionSummaryAnalyser,
    private val manifestSummaryAnalyser: ManifestSummaryAnalyser,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val sargonOsManager: SargonOsManager,
    private val getProfileUseCase: GetProfileUseCase
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    /**
     * Runs analysis on each transaction type received. Resolves the preview type and returns the signers involved.
     */
    suspend fun analyse(): Result<Analysis> = when (data.value.request.transactionType) {
        is TransactionType.PreAuthorized -> analysePreAuthTransaction(
            manifestData = data.value.request.unvalidatedManifestData
        )

        else -> analyseTransaction(
            manifestData = data.value.request.unvalidatedManifestData,
            isInternal = data.value.request.isInternal
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
        manifestData: UnvalidatedManifestData,
        isInternal: Boolean
    ): Result<Analysis> = runCatching {
        val notary = data.value.ephemeralNotaryPrivateKey
        val transactionToReview = sargonOsManager.sargonOs.analyseTransactionPreview(
            instructions = manifestData.instructions,
            blobs = Blobs.init(blobs = manifestData.blobs.map { Blob.init(it) }),
            areInstructionsOriginatingFromHost = isInternal,
            nonce = Nonce.secureRandom(),
            notaryPublicKey = notary.toPublicKey()
        )

        val profile = getProfileUseCase()
        val summary = Summary.FromExecution(
            manifest = SummarizedManifest.Transaction(transactionToReview.transactionManifest),
            summary = transactionToReview.executionSummary
        )
        val previewType = executionSummaryAnalyser.analyze(summary)

        Analysis(
            previewType = previewType,
            summary = summary,
            profile = profile
        )
    }

    private suspend fun analysePreAuthTransaction(
        manifestData: UnvalidatedManifestData,
    ): Result<Analysis> = runCatching {
        val preAuthToReview = sargonOsManager.sargonOs.analysePreAuthPreview(
            instructions = manifestData.instructions,
            blobs = Blobs.init(blobs = manifestData.blobs.map { Blob.init(it) }),
            nonce = Nonce.secureRandom(),
        )

        val profile = getProfileUseCase()

        when (preAuthToReview) {
            is PreAuthToReview.Enclosed -> {
                val summary = Summary.FromExecution(
                    manifest = SummarizedManifest.Subintent(preAuthToReview.v1.manifest),
                    summary = preAuthToReview.v1.summary
                )
                val previewType = executionSummaryAnalyser.analyze(summary)

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
                val previewType = manifestSummaryAnalyser.analyze(summary = summary)

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
