package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.SearchFeePayersUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.flow.update
import rdx.works.core.decodeHex
import rdx.works.core.then
import rdx.works.core.toUByteList
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val resolveDAppsUseCase: ResolveDAppsUseCase,
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
    ): Result<Unit> {
        val notaryAndSigners = transactionClient.getNotaryAndSigners(
            manifest = manifest,
            ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey
        )
        return transactionClient.getTransactionPreview(
            manifest = manifest,
            notaryAndSigners = notaryAndSigners
        ).mapCatching { preview ->
            manifest
                .analyzeExecution(transactionReceipt = preview.encodedReceipt.decodeHex().toUByteList())
                .resolvePreview(notaryAndSigners)
                .resolveFees(notaryAndSigners)
        }.mapCatching { transactionFees ->
            val feePayerResult = searchFeePayersUseCase(
                manifest = manifest,
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

    private suspend fun ExecutionAnalysis.resolvePreview(notaryAndSigners: NotaryAndSigners) = apply {
        val previewType = if (_state.value.requestNonNull.isInternal.not() && reservedInstructions.isNotEmpty()) {
            // wallet unacceptable manifest
            _state.update {
                it.copy(
                    error = UiMessage.ErrorMessage(RadixWalletException.DappRequestException.UnacceptableManifest)
                )
            }
            PreviewType.UnacceptableManifest
        } else if (transactionTypes.isEmpty()) {
            PreviewType.NonConforming
        } else {
            processConformingManifest(transactionTypes[0])
        }

        _state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = previewType,
                defaultSignersCount = notaryAndSigners.signers.count()
            )
        }
    }

    private fun ExecutionAnalysis.resolveFees(notaryAndSigners: NotaryAndSigners) = TransactionFees(
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

    private suspend fun processConformingManifest(transactionType: TransactionType): PreviewType {
        val resources = getResourcesUseCase(addresses = transactionType.involvedResourceAddresses).getOrThrow()

        return when (transactionType) {
            is TransactionType.GeneralTransaction -> transactionType.resolve(
                resources = resources,
                getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                getProfileUseCase = getProfileUseCase,
                resolveDAppsUseCase = resolveDAppsUseCase
            )

            is TransactionType.SimpleTransfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                resources = resources
            )

            is TransactionType.Transfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                resources = resources
            )

            is TransactionType.AccountDepositSettings -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                allResources = resources
            )

            else -> PreviewType.NonConforming
        }
    }

    private fun reportFailure(error: Throwable) {
        logger.w(error)

        _state.update {
            it.copy(
                isLoading = false,
                isNetworkFeeLoading = false,
                previewType = PreviewType.None,
                error = UiMessage.ErrorMessage(error)
            )
        }
    }
}
