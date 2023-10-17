package com.babylon.wallet.android.presentation.transaction.analysis

import com.babylon.wallet.android.data.transaction.NotaryAndSigners
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.domain.usecases.GetAccountsWithAssetsUseCase
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.usecases.GetAccountsWithResourcesUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesMetadataUseCase
import com.babylon.wallet.android.domain.usecases.GetResourcesUseCase
import com.babylon.wallet.android.domain.usecases.ResolveDAppsUseCase
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
import rdx.works.core.then
import rdx.works.profile.domain.GetProfileUseCase
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionAnalysisDelegate @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getAccountsWithAssetsUseCase: GetAccountsWithAssetsUseCase,
    private val getResourcesMetadataUseCase: GetResourcesMetadataUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val getTransactionBadgesUseCase: GetTransactionBadgesUseCase,
    private val resolveDAppsUseCase: ResolveDAppsUseCase,
    private val transactionClient: TransactionClient
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionAnalysis")

    suspend fun analyse() {
        _state.value.requestNonNull.transactionManifestData.toTransactionManifest().onSuccess {
            startAnalysis(it)
        }.onFailure { error ->
            reportFailure(error)
        }
    }

    private suspend fun startAnalysis(manifest: TransactionManifest) {
        val notaryAndSigners = transactionClient.getNotaryAndSigners(
            manifest = manifest,
            ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey
        )
        transactionClient.getTransactionPreview(
            manifest = manifest,
            notaryAndSigners = notaryAndSigners
        ).then { preview ->
            transactionClient.analyzeExecution(manifest, preview)
        }.resolve(manifest, notaryAndSigners)
    }

    @Suppress("LongMethod")
    private suspend fun Result<ExecutionAnalysis>.resolve(
        manifest: TransactionManifest,
        notaryAndSigners: NotaryAndSigners
    ) = this.onSuccess { analysis ->
        val previewType = if (_state.value.requestNonNull.isInternal.not() &&
            analysis.reservedInstructions.isNotEmpty()
        ) {
            // wallet unacceptable manifest
            _state.update {
                it.copy(
                    error = UiMessage.ErrorMessage(RadixWalletException.DappRequestException.UnacceptableManifest)
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

        _state.update {
            it.copy(
                isRawManifestVisible = previewType == PreviewType.NonConforming,
                isLoading = false,
                previewType = previewType,
                defaultSignersCount = notaryAndSigners.signers.count()
            )
        }

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
            _state.update {
                it.copy(
                    isNetworkFeeLoading = false,
                    transactionFees = transactionFees,
                    feePayerSearchResult = feePayerResult
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
                getAccountsWithAssetsUseCase = getAccountsWithAssetsUseCase,
                getResourcesMetadataUseCase = getResourcesMetadataUseCase,
                resolveDAppsUseCase = resolveDAppsUseCase
            )

            is TransactionType.SimpleTransfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithAssetsUseCase = getAccountsWithAssetsUseCase
            )

            is TransactionType.Transfer -> transactionType.resolve(
                getProfileUseCase = getProfileUseCase,
                getAccountsWithAssetsUseCase = getAccountsWithAssetsUseCase
            )

            is TransactionType.AccountDepositSettings -> transactionType.resolve(getProfileUseCase, getResourcesUseCase)

            else -> {
                PreviewType.NonConforming
            }
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
