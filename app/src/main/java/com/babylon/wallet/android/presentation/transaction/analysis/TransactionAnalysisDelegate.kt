package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
import com.babylon.wallet.android.domain.usecases.transaction.GetTransactionBadgesUseCase
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State
import com.babylon.wallet.android.presentation.transaction.fees.TransactionFees
import com.babylon.wallet.android.presentation.transaction.guaranteesCount
import com.radixdlt.ret.ExecutionAnalysis
import com.radixdlt.ret.TransactionManifest
import com.radixdlt.ret.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LongParameterList")
class TransactionAnalysisDelegate(
    private val state: MutableStateFlow<State>,
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsWithResourcesUseCase: GetAccountsWithResourcesUseCase,
    private val getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
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

    private suspend fun startAnalysis(manifest: TransactionManifest) {
        val notaryAndSigners = transactionClient.getNotaryAndSigners(
            manifest = manifest,
            ephemeralNotaryPrivateKey = state.value.ephemeralNotaryPrivateKey
        )
        transactionClient.getTransactionPreview(
            manifest = manifest,
            notaryAndSigners = notaryAndSigners
        ).then { preview ->
            transactionClient.analyzeExecution(manifest, preview)
        }.resolve(manifest, notaryAndSigners)
    }

    private suspend fun Result<ExecutionAnalysis>.resolve(
        manifest: TransactionManifest,
        notaryAndSigners: NotaryAndSigners
    ) = this.onSuccess { analysis ->
        val previewType = if (analysis.reservedInstructions.isNotEmpty()) { // wallet unacceptable manifest
            state.update {
                it.copy(
                    error = UiMessage.ErrorMessage.from(DappRequestException(DappRequestFailure.UnacceptableManifest))
                )
            }
            PreviewType.UnacceptableManifest
        } else if (analysis.transactionTypes.isEmpty()) {
            PreviewType.NonConforming
        } else {
            processConformingManifest(analysis.transactionTypes[0])
        }

        var transactionFees = TransactionFees(
            nonContingentFeeLock = analysis.feeLocks.lock.asStr().toBigDecimal(),
            networkExecution = analysis.feeSummary.executionCost.asStr().toBigDecimal(),
            networkFinalization = analysis.feeSummary.finalizationCost.asStr().toBigDecimal(),
            networkStorage = analysis.feeSummary.storageExpansionCost.asStr().toBigDecimal(),
            royalties = analysis.feeSummary.royaltyCost.asStr().toBigDecimal(),
            guaranteesCount = (previewType as? PreviewType.Transfer)?.to?.guaranteesCount() ?: 0,
            notaryIsSignatory = notaryAndSigners.notaryIsSignatory,
            includeLockFee = false, // First its false because we don't know if lock fee is applicable or not yet
            signersCount = notaryAndSigners.signers.count()
        )

        if (transactionFees.defaultTransactionFee > BigDecimal.ZERO) {
            // There will be a lock fee so update lock fee cost
            transactionFees = transactionFees.copy(
                includeLockFee = true
            )
        }

        transactionClient.findFeePayerInManifest(
            manifest = manifest,
            lockFee = transactionFees.defaultTransactionFee
        ).onSuccess { feePayerResult ->
            state.update {
                it.copy(
                    isRawManifestVisible = previewType == PreviewType.NonConforming,
                    transactionFees = transactionFees,
                    isLoading = false,
                    previewType = previewType,
                    feePayerSearchResult = feePayerResult.copy(
                        insufficientBalanceToPayTheFee = feePayerResult.candidateXrdBalance() < transactionFees.defaultTransactionFee
                    ),
                    defaultSignersCount = notaryAndSigners.signers.count()
                )
            }
        }
    }.onFailure { error ->
        reportFailure(error)
    }

    private suspend fun processConformingManifest(transactionType: TransactionType): PreviewType {
        return when (transactionType) {

            is TransactionType.GeneralTransaction -> transactionType.resolve(
                getTransactionBadgesUseCase = getTransactionBadgesUseCase,
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase,
                getResourcesMetadataUseCase = getResourcesMetadataUseCase,
                resolveDAppsUseCase = resolveDAppsUseCase
            )

            is TransactionType.SimpleTransfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
            )

            is TransactionType.Transfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithResourcesUseCase = getAccountsWithResourcesUseCase
            )

            is TransactionType.AccountDepositSettings -> transactionType.resolve(getProfileUseCase, getResourcesUseCase)

            else -> {
                PreviewType.NonConforming
            }
        }
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
