package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.gateway.extensions.asGatewayPublicKey
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequest
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewRequestFlags
import com.babylon.wallet.android.data.gateway.generated.models.TransactionPreviewResponse
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.ResolveNotaryAndSignersUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.assets.CacheNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.processor.PreviewTypeAnalyzer
import com.radixdlt.hex.extensions.toHexString
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.Nonce
import com.radixdlt.sargon.extensions.secureRandom
import com.radixdlt.sargon.extensions.value
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.then
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val previewTypeAnalyzer: PreviewTypeAnalyzer,
    private val cacheNewlyCreatedEntitiesUseCase: CacheNewlyCreatedEntitiesUseCase,
    private val resolveNotaryAndSignersUseCase: ResolveNotaryAndSignersUseCase,
    private val searchFeePayersUseCase: SearchFeePayersUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse() {
        val manifestData = _state.value.requestNonNull.transactionManifestData.also {
            logger.v(it.instructions)
        }
        startAnalysis(manifestData)
    }

    private suspend fun startAnalysis(manifestData: TransactionManifestData) {
        runCatching {
            manifestData.entitiesRequiringAuth()
        }.then { entitiesRequiringAuth ->
            resolveNotaryAndSignersUseCase(
                accountsAddressesRequiringAuth = entitiesRequiringAuth.accounts,
                personaAddressesRequiringAuth = entitiesRequiringAuth.identities,
                notary = _state.value.ephemeralNotaryPrivateKey
            )
        }.then { notaryAndSigners ->
            getTransactionPreview(
                manifestData = manifestData,
                notaryAndSigners = notaryAndSigners
            ).mapCatching { preview ->
                logger.v(preview.encodedReceipt)
                manifestData
                    .executionSummary(encodedReceipt = preview.encodedReceipt.decodeHex())
                    .resolvePreview(notaryAndSigners)
                    .resolveFees(notaryAndSigners)
            }
        }.then { transactionFees ->
            _state.update { it.copy(transactionFees = transactionFees) }

            searchFeePayersUseCase(
                manifestData = manifestData,
                lockFee = transactionFees.defaultTransactionFee
            )
        }.onSuccess { feePayers ->
            _state.update { it.copy(isNetworkFeeLoading = false, feePayers = feePayers) }
        }.onFailure { error ->
            reportFailure(error)
        }
    }

    private suspend fun getTransactionPreview(
        manifestData: TransactionManifestData,
        notaryAndSigners: NotaryAndSigners
    ): Result<TransactionPreviewResponse> {
        val (startEpochInclusive, endEpochExclusive) = with(transactionRepository.getLedgerEpoch()) {
            val epoch = this.getOrNull() ?: return@with (0.toULong() to 0.toULong())

            (epoch to epoch + 1.toULong())
        }

        return transactionRepository.getTransactionPreview(
            TransactionPreviewRequest(
                manifest = manifestData.instructions,
                startEpochInclusive = startEpochInclusive.toLong(),
                endEpochExclusive = endEpochExclusive.toLong(),
                tipPercentage = 0,
                nonce = Nonce.secureRandom().value.toLong(),
                signerPublicKeys = notaryAndSigners.signersPublicKeys().map { it.asGatewayPublicKey() },
                flags = TransactionPreviewRequestFlags(
                    useFreeCredit = true,
                    assumeAllSignatureProofs = false,
                    skipEpochCheck = false
                ),
                blobsHex = manifestData.blobs.map { it.toHexString() },
                notaryPublicKey = notaryAndSigners.notaryPublicKeyNew().asGatewayPublicKey(),
                notaryIsSignatory = notaryAndSigners.notaryIsSignatory
            )
        ).fold(
            onSuccess = { preview ->
                if (preview.receipt.isFailed) {
                    val errorMessage = preview.receipt.errorMessage.orEmpty()
                    val isFailureDueToDepositRules = errorMessage.contains("AccountError(DepositIsDisallowed") ||
                        errorMessage.contains("AccountError(NotAllBucketsCouldBeDeposited")
                    if (isFailureDueToDepositRules) {
                        Result.failure(RadixWalletException.PrepareTransactionException.ReceivingAccountDoesNotAllowDeposits)
                    } else {
                        Result.failure(Throwable(preview.receipt.errorMessage))
                    }
                } else {
                    Result.success(preview)
                }
            },
            onFailure = { Result.failure(it) }
        )
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
