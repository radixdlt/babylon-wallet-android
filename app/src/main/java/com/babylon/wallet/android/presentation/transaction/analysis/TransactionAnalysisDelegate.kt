package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.assets.GetFiatValueUseCase
import com.babylon.wallet.android.domain.usecases.signing.NotaryAndSigners
import com.babylon.wallet.android.domain.usecases.signing.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PreviewTypeAnalyzer
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.extensions.summary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.then
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val previewTypeAnalyzer: PreviewTypeAnalyzer,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase,
    private val getFiatValueUseCase: GetFiatValueUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse() {
        val manifestData = _state.value.requestNonNull.transactionManifestData.also {
            logger.v(it.instructions)
        }
        startAnalysis(manifestData)
        fetchXrdPrice()
    }

    private fun fetchXrdPrice() {
        viewModelScope.launch {
            getFiatValueUseCase.forXrd().onSuccess { fiatPrice ->
                _state.update { state ->
                    state.copy(transactionFees = state.transactionFees.copy(xrdFiatPrice = fiatPrice))
                }
            }
        }
    }

    private suspend fun startAnalysis(manifestData: TransactionManifestData) {
        withContext(defaultDispatcher) {
            runCatching {
                transactionRepository.analyzeTransaction(
                    manifestData = manifestData,
                    isWalletTransaction = _state.value.requestNonNull.isInternal,
                    notaryPublicKey = _state.value.ephemeralNotaryPrivateKey.toPublicKey()
                )
            }.then { transactionToReview ->
                _state.update { it.copy(transactionManifestData = TransactionManifestData.from(transactionToReview.transactionManifest)) }

                val manifestSummary = requireNotNull(transactionToReview.transactionManifest.summary)
                resolveNotaryAndSignersUseCase(
                    accountsAddressesRequiringAuth = manifestSummary.addressesOfAccountsRequiringAuth,
                    personaAddressesRequiringAuth = manifestSummary.addressesOfPersonasRequiringAuth,
                    notary = _state.value.ephemeralNotaryPrivateKey
                ).map { notaryAndSigners ->
                    transactionToReview.executionSummary
                        .resolvePreview(notaryAndSigners)
                        .resolveFees(notaryAndSigners)
                }
            }.then { transactionFees ->
                _state.update { it.copy(transactionFees = transactionFees) }

                searchFeePayersUseCase(
                    feePayerCandidates = _state.value.feePayerCandidates,
                    lockFee = transactionFees.defaultTransactionFee
                )
            }.onSuccess { feePayers ->
                _state.update {
                    it.copy(
                        isNetworkFeeLoading = false,
                        feePayers = feePayers
                    )
                }
            }.onFailure { error ->
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
                        reportFailure(RadixWalletException.DappRequestException.PreviewError(error))
                    }
                }
            }
        }
    }

    private suspend fun ExecutionSummary.resolvePreview(notaryAndSigners: NotaryAndSigners) = apply {
        val previewType = previewTypeAnalyzer.analyze(this)

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

        _state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                showRawTransactionWarning = previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = previewType,
                defaultSignersCount = notaryAndSigners.signers.count()
            )
        }
    }

    private fun ExecutionSummary.resolveFees(notaryAndSigners: NotaryAndSigners) = FeesResolver.resolve(
        summary = this,
        notaryAndSigners = notaryAndSigners,
        previewType = _state.value.previewType
    )

    private fun reportFailure(error: Throwable) {
        logger.w(error)

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
