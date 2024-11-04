package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.signing.ResolveSignersUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.ExecutionSummaryAnalyser
import com.babylon.wallet.android.presentation.transaction.analysis.summary.manifest.ManifestSummaryAnalyser
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.Blob
import com.radixdlt.sargon.Blobs
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.PreAuthToReview
import com.radixdlt.sargon.extensions.ProfileEntity
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.secureRandom
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.flow.update
import rdx.works.core.sargon.formatted
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val executionSummaryAnalyser: ExecutionSummaryAnalyser,
    private val manifestSummaryAnalyser: ManifestSummaryAnalyser,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val resolveSignersUseCase: ResolveSignersUseCase,
    private val sargonOsManager: SargonOsManager,
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    /**
     * Runs analysis on each transaction type received. Resolves the preview type and returns the signers involved.
     */
    suspend fun analyse(): Result<List<ProfileEntity>> = when (val transactionType = data.value.request.transactionType) {
        is TransactionType.PreAuthorized -> analysePreAuthTransaction(
            manifestData = data.value.request.unvalidatedManifestData,
            expiration = transactionType.expiration
        )

        else -> analyseTransaction(
            manifestData = data.value.request.unvalidatedManifestData,
            isInternal = data.value.request.isInternal
        )
    }.onSuccess { previewAndSigners ->
        cacheNewlyCreatedResources(previewType = previewAndSigners.first)
    }.onSuccess { previewAndSigners ->
        val (previewType, signers) = previewAndSigners

        data.update { it.copy(signers = signers) }

        _state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                showRawTransactionWarning = previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = previewType,
            )
        }
    }.onFailure { error ->
        onError(error)
    }.map { it.second }

    private suspend fun analyseTransaction(
        manifestData: UnvalidatedManifestData,
        isInternal: Boolean
    ): Result<Pair<PreviewType, List<ProfileEntity>>> = runCatching {
        val notary = data.value.ephemeralNotaryPrivateKey
        val transactionToReview = sargonOsManager.sargonOs.analyseTransactionPreview(
            instructions = manifestData.instructions,
            blobs = Blobs.init(blobs = manifestData.blobs.map { Blob.init(it) }),
            areInstructionsOriginatingFromHost = isInternal,
            nonce = Nonce.secureRandom(),
            notaryPublicKey = notary.toPublicKey()
        )

        val signers = resolveSignersUseCase(summary = transactionToReview.executionSummary).getOrThrow()

        executionSummaryAnalyser.analyse(transactionToReview.executionSummary) to signers
    }

    private suspend fun analysePreAuthTransaction(
        manifestData: UnvalidatedManifestData,
        expiration: DappToWalletInteractionSubintentExpiration?
    ): Result<Pair<PreviewType, List<ProfileEntity>>> = runCatching {
        val preAuthToReview = sargonOsManager.sargonOs.analysePreAuthPreview(
            instructions = manifestData.instructions,
            blobs = Blobs.init(blobs = manifestData.blobs.map { Blob.init(it) }),
            nonce = Nonce.secureRandom(),
        )

        when (preAuthToReview) {
            is PreAuthToReview.Enclosed -> {
                val previewType = executionSummaryAnalyser.analyse(summary = preAuthToReview.v1.summary)

                previewType to resolveSignersUseCase(summary = preAuthToReview.v1.summary).getOrThrow()
            }

            is PreAuthToReview.Open -> {
                val previewType = manifestSummaryAnalyser.analyse(summary = preAuthToReview.v1.summary, expiration = expiration)

                previewType to resolveSignersUseCase(summary = preAuthToReview.v1.summary).getOrThrow()
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
