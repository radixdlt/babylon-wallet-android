package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.signing.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PreviewTypeAnalyzer
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.summary
import kotlinx.coroutines.flow.update
import rdx.works.core.then
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val previewTypeAnalyzer: PreviewTypeAnalyzer,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse() {
        val manifestData = data.value.request.unvalidatedManifestData.also {
            logger.v(it.instructions)
        }

        runCatching {
            transactionRepository.analyzeTransaction(
                manifestData = manifestData,
                isWalletTransaction = data.value.request.isInternal,
                notaryPublicKey = data.value.ephemeralNotaryPrivateKey.toPublicKey()
            )
        }.then { transactionToReviewData ->
            val manifestSummary = transactionToReviewData.transactionToReview.transactionManifest.summary!!
            resolveNotaryAndSignersUseCase(
                accountsAddressesRequiringAuth = manifestSummary.addressesOfAccountsRequiringAuth,
                personaAddressesRequiringAuth = manifestSummary.addressesOfPersonasRequiringAuth,
                notary = data.value.ephemeralNotaryPrivateKey
            ).onSuccess { notaryAndSigners ->
                data.update {
                    it.copy(
                        txToReviewData = transactionToReviewData,
                        txNotaryAndSigners = notaryAndSigners
                    )
                }
            }.map {
                transactionToReviewData.transactionToReview.executionSummary
            }
        }.onSuccess { executionSummary ->
            val previewType = resolvePreview(executionSummary)
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
        }
    }

    private suspend fun resolvePreview(executionSummary: ExecutionSummary): PreviewType {
        return previewTypeAnalyzer.analyze(executionSummary).also { previewType ->
            if (previewType is PreviewType.Transfer) {
                val newlyCreatedResources = previewType.newlyCreatedResources
                if (newlyCreatedResources.isNotEmpty()) {
                    cacheNewlyCreatedEntitiesUseCase.forResources(newlyCreatedResources)
                }
                val newlyCreatedNFTItemsForExistingResources = previewType.newlyCreatedNFTItemsForExistingResources
                if (newlyCreatedNFTItemsForExistingResources.isNotEmpty()) {
                    cacheNewlyCreatedEntitiesUseCase.forNFTs(newlyCreatedNFTItemsForExistingResources)
                }
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
            else -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        isNetworkFeeLoading = false,
                        previewType = PreviewType.None,
                        error = TransactionErrorMessage(error)
                    )
                }
            }
        }
    }
}
