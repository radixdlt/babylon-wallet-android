package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.dapp.model.TransactionType
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
import com.radixdlt.sargon.DappToWalletInteractionSubintentExpiration
import com.radixdlt.sargon.ExecutionSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.then
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZonedDateTime
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

        transactionRepository.analyzeTransaction(
            manifestData = manifestData,
            isWalletTransaction = data.value.request.isInternal,
            notaryPublicKey = data.value.ephemeralNotaryPrivateKey.toPublicKey()
        ).then { transactionToReviewData ->
            resolveNotaryAndSignersUseCase(
                accountsAddressesRequiringAuth = transactionToReviewData.manifestSummary.addressesOfAccountsRequiringAuth,
                personaAddressesRequiringAuth = transactionToReviewData.manifestSummary.addressesOfPersonasRequiringAuth,
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
            val transactionType = data.value.request.transactionType
            _state.update {
                it.copy(
                    transactionType = when (transactionType) {
                        is TransactionType.PreAuthorized -> TransactionReviewViewModel.State.TransactionType.PreAuthorized
                        else -> TransactionReviewViewModel.State.TransactionType.Regular
                    },
                    isRawManifestVisible = previewType == PreviewType.NonConforming,
                    showRawTransactionWarning = previewType == PreviewType.NonConforming,
                    isLoading = false,
                    previewType = previewType,
                    submit = TransactionReviewViewModel.State.Submit(
                        isVisible = true,
                        isEnabled = previewType != PreviewType.NonConforming && previewType != PreviewType.UnacceptableManifest,
                        isLoading = false
                    )
                )
            }

            (transactionType as? TransactionType.PreAuthorized)?.expiration?.let { expiration ->
                startTimer(expiration)
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

    private fun startTimer(expiration: DappToWalletInteractionSubintentExpiration) {
        viewModelScope.launch {
            val remainingSeconds = when (expiration) {
                is DappToWalletInteractionSubintentExpiration.AfterDelay -> {
                    expiration.v1.expireAfterSeconds.toLong()
                }
                is DappToWalletInteractionSubintentExpiration.AtTime -> {
                    ZonedDateTime.now().toEpochSecond() - expiration.v1.unixTimestampSeconds.toEpochSecond()
                }
            }

            _state.update {
                it.copy(
                    preAuthorization = TransactionReviewViewModel.State.PreAuthorization(
                        expiration = TransactionReviewViewModel.State.PreAuthorization.Expiration(
                            isExpiringAtTime = expiration is DappToWalletInteractionSubintentExpiration.AtTime,
                            remainingSeconds = remainingSeconds
                        )
                    )
                )
            }

            while (remainingSeconds >= 0) {
                delay(1000)
                _state.update { state ->
                    state.copy(
                        preAuthorization = state.preAuthorization?.copy(
                            expiration = state.preAuthorization.expiration?.copy(
                                remainingSeconds = remainingSeconds
                            )
                        ),
                        submit = state.submit.copy(
                            isVisible = remainingSeconds > 0
                        )
                    )
                }
            }
        }
    }
}
