package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PreviewTypeAnalyzer
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.ExecutionSummary
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.currentNetwork
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val previewTypeAnalyzer: PreviewTypeAnalyzer,
    private val getProfileUseCase: GetProfileUseCase,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse(transactionClient: TransactionClient) {
        _state.value.requestNonNull.transactionManifestData
            .toTransactionManifest()
            .then {
                startAnalysis(it, transactionClient)
            }.onFailure { error ->
                reportFailure(error)
            }
    }

    private suspend fun startAnalysis(
        manifest: TransactionManifest,
        transactionClient: TransactionClient
    ): Result<Unit> = runCatching {
        getProfileUseCase.currentNetwork()?.networkID ?: error("No network found")
    }.then { networkId ->
        val summary = manifest.summary(networkId.toUByte())

        resolveNotaryAndSignersUseCase(
            summary = summary,
            notary = _state.value.ephemeralNotaryPrivateKey
        ).then { notaryAndSigners ->
            transactionClient.getTransactionPreview(
                manifest = manifest,
                notaryAndSigners = notaryAndSigners
            ).mapCatching { preview ->
                logger.d(preview.encodedReceipt)
                manifest
                    .executionSummary(
                        networkId = networkId.toUByte(),
                        encodedReceipt = preview.encodedReceipt.decodeHex()
                    )
                    .resolvePreview(notaryAndSigners)
                    .resolveFees(notaryAndSigners)
            }.mapCatching { transactionFees ->
                val feePayerResult = searchFeePayersUseCase(
                    manifestSummary = summary,
                    lockFee = transactionFees.defaultTransactionFee
                ).getOrThrow()

                _state.update {
                    it.copy(
                        isNetworkFeeLoading = false,
                        transactionFees = transactionFees,
                        feePayerSearchResult = feePayerResult
                    )
                }
            }
        }
    }

    private suspend fun ExecutionSummary.resolvePreview(notaryAndSigners: NotaryAndSigners) = apply {
        val previewType = if (_state.value.requestNonNull.isInternal.not() && reservedInstructions.isNotEmpty()) {
            // wallet unacceptable manifest
            _state.update {
                it.copy(
                    error = TransactionErrorMessage(RadixWalletException.DappRequestException.UnacceptableManifest)
                )
            }
            PreviewType.UnacceptableManifest
        } else {
            previewTypeAnalyzer.analyze(this)
        }

        if (previewType is PreviewType.Transfer) {
            val newlyCreated = previewType.newlyCreatedResources
            if (newlyCreated.isNotEmpty()) {
                cacheNewlyCreatedEntitiesUseCase(newlyCreated)
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

    private fun ExecutionSummary.resolveFees(notaryAndSigners: NotaryAndSigners) = TransactionFees(
        nonContingentFeeLock = feeLocks.lock.asStr().toBigDecimal(),
        networkExecution = feeSummary.executionCost.asStr().toBigDecimal(),
        networkFinalization = feeSummary.finalizationCost.asStr().toBigDecimal(),
        networkStorage = feeSummary.storageExpansionCost.asStr().toBigDecimal(),
        royalties = feeSummary.royaltyCost.asStr().toBigDecimal(),
        guaranteesCount = (_state.value.previewType as? PreviewType.Transfer)?.to?.guaranteesCount() ?: 0,
        notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
        includeLockFee = false, // First its false because we don't know if lock fee is applicable or not yet
        signersCount = notaryAndSigners.signers.count()
    ).let { fees ->
        if (fees.defaultTransactionFee > BigDecimal.ZERO) {
            // There will be a lock fee so update lock fee cost
            fees.copy(includeLockFee = true)
        } else {
            fees
        }
    }

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
