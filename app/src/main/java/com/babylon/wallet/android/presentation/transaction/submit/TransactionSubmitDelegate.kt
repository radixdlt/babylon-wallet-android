package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.SubintentExpiration
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.toDappWalletInteractionErrorType
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.assets.ClearCachedNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignAndNotariseTransactionUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignSubintentUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.model.BoundedAmount
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummarizedManifest
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.SignedSubintent
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.TransactionGuarantee
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.modifyAddGuarantees
import com.radixdlt.sargon.extensions.then
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Asset
import rdx.works.core.logNonFatalException
import rdx.works.core.mapError
import rdx.works.core.toUnitResult
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import javax.inject.Inject

interface TransactionSubmitDelegate {

    fun onApproveTransaction()
}

@Suppress("LongParameterList")
class TransactionSubmitDelegateImpl @Inject constructor(
    private val signAndNotarizeTransactionUseCase: SignAndNotariseTransactionUseCase,
    private val signSubintentUseCase: SignSubintentUseCase,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionRepository: TransactionRepository,
    private val clearCachedNewlyCreatedEntitiesUseCase: ClearCachedNewlyCreatedEntitiesUseCase,
    private val appEventBus: AppEventBus,
    private val transactionStatusClient: TransactionStatusClient,
    private val exceptionMessageProvider: ExceptionMessageProvider,
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>(),
    TransactionSubmitDelegate {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    var oneOffEventHandler: OneOffEventHandler<TransactionReviewViewModel.Event>? = null

    override fun onApproveTransaction() {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = viewModelScope.launch {
            val currentNetworkId = getCurrentGatewayUseCase().network.id
            val manifestNetworkId = data.value.summary.networkId

            if (currentNetworkId != manifestNetworkId) {
                onDismiss(
                    exception = RadixWalletException.DappRequestException.WrongNetwork(
                        currentNetworkId = currentNetworkId,
                        requestNetworkId = manifestNetworkId
                    )
                )
                return@launch
            }

            prepareSummary()
                .then { summary ->
                    signAndSubmit(summary = summary)
                }.onSuccess {
                    approvalJob = null

                    val previewType = _state.value.previewType as? PreviewType.Transaction ?: return@onSuccess
                    clearCachedNewlyCreatedEntitiesUseCase(previewType.newlyCreatedNFTs)
                }.onFailure { error ->
                    handleSignAndSubmitFailure(error)
                }
        }
    }

    suspend fun onDismiss(exception: RadixWalletException.DappRequestException): Result<Unit> = runCatching {
        if (approvalJob == null) {
            val request = data.value.request
            if (!request.isInternal) {
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = request,
                    dappWalletInteractionErrorType = exception.dappWalletInteractionErrorType,
                    message = exception.getDappMessage()
                )
            }
            oneOffEventHandler?.sendEvent(TransactionReviewViewModel.Event.Dismiss)
            incomingRequestRepository.requestHandled(request.interactionId)
        } else {
            logger.d("Cannot dismiss transaction while is in progress")
        }
    }

    private fun prepareSummary(): Result<Summary> = runCatching {
        when (val summary = data.value.summary) {
            is Summary.FromExecution -> {
                val transactionPreviewType = (_state.value.previewType as? PreviewType.Transaction)
                val transactionManifest = (summary.manifest as? SummarizedManifest.Transaction)?.manifest
                if (transactionPreviewType != null && transactionManifest != null) {
                    summary.copy(
                        manifest = SummarizedManifest.Transaction(
                            transactionManifest.addAssertions(deposits = transactionPreviewType.to)
                        )
                    )
                } else {
                    summary
                }
            }

            else -> summary
        }
    }.mapError { RadixWalletException.PrepareTransactionException.ConvertManifest }

    private suspend fun signAndSubmit(summary: Summary): Result<Unit> {
        _state.update { it.copy(isSubmitting = true) }

        return when (summary) {
            is Summary.FromExecution -> when (summary.manifest) {
                is SummarizedManifest.Subintent -> signAndSubmit(subintentManifest = summary.manifest.manifest)
                is SummarizedManifest.Transaction -> signAndSubmit(transactionManifest = summary.manifest.manifest)
            }
            is Summary.FromStaticAnalysis -> signAndSubmit(subintentManifest = summary.manifest.manifest)
        }.toUnitResult()
    }

    private suspend fun signAndSubmit(transactionManifest: TransactionManifest): Result<Unit> {
        val fees = _state.value.fees ?: error("Fees were not resolved")
        val transactionRequest = data.value.request
        val transactionRequestKind = transactionRequest.kind as? TransactionRequest.Kind.Regular
            ?: error("Wrong kind: ${transactionRequest.kind}")
        val feePayerAddress = data.value.feePayers?.selectedAccountAddress

        return signAndNotarizeTransactionUseCase(
            manifest = transactionManifest,
            networkId = transactionRequest.unvalidatedManifestData.networkId,
            message = transactionRequest.unvalidatedManifestData.message,
            lockFee = fees.transactionFees.transactionFeeToLock,
            tipPercentage = fees.transactionFees.tipPercentageForTransaction,
            notarySecretKey = data.value.ephemeralNotaryPrivateKey,
            feePayerAddress = feePayerAddress
        ).then { notarizationResult ->
            transactionRepository.submitTransaction(notarizationResult.notarizedTransaction).map { notarizationResult }
        }.onSuccess { notarization ->
            _state.update { it.copy(isSubmitting = false) }

            appEventBus.sendEvent(
                AppEvent.Status.Transaction.InProgress(
                    requestId = transactionRequest.interactionId,
                    transactionId = notarization.intentHash.bech32EncodedTxId,
                    isInternal = transactionRequest.isInternal,
                    blockUntilComplete = transactionRequest.blockUntilComplete,
                    isMobileConnect = transactionRequest.isMobileConnectRequest,
                    dAppName = _state.value.proposingDApp?.name
                )
            )

            transactionStatusClient.startPollingForTransactionStatus(
                intentHash = notarization.intentHash,
                requestId = data.value.request.interactionId,
                transactionType = transactionRequestKind.transactionType,
                endEpoch = notarization.endEpoch
            )

            // Respond to dApp
            if (!data.value.request.isInternal) {
                respondToIncomingRequestUseCase.respondWithSuccessTransactionIntent(
                    request = data.value.request,
                    intentHash = notarization.intentHash
                )
            }
        }.toUnitResult()
    }

    private suspend fun signAndSubmit(subintentManifest: SubintentManifest): Result<SignedSubintent> {
        val transactionRequest = data.value.request
        val transactionRequestKind = transactionRequest.kind as? TransactionRequest.Kind.PreAuthorized
            ?: error("Wrong kind: ${transactionRequest.kind}")

        return signSubintentUseCase(
            manifest = subintentManifest,
            message = transactionRequest.unvalidatedManifestData.plainMessage,
            expiration = transactionRequestKind.expiration
        ).onSuccess { signedSubintent ->
            _state.update { it.copy(isSubmitting = false) }

            appEventBus.sendEvent(
                AppEvent.Status.PreAuthorization.Sent(
                    requestId = transactionRequest.interactionId,
                    preAuthorizationId = signedSubintent.subintent.hash().bech32EncodedTxId,
                    isMobileConnect = transactionRequest.isMobileConnectRequest,
                    dAppName = _state.value.proposingDApp?.name,
                    remainingTime = when (val expiration = transactionRequestKind.expiration) {
                        is SubintentExpiration.AtTime -> expiration.expirationDuration()
                        is SubintentExpiration.DelayAfterSign -> expiration.delay
                    }
                )
            )

            transactionStatusClient.startPollingForPreAuthorizationStatus(
                intentHash = signedSubintent.subintent.hash(),
                requestId = data.value.request.interactionId,
                expiration = transactionRequestKind.expiration.toDAppInteraction()
            )

            // Respond to dApp
            if (!data.value.request.isInternal) {
                respondToIncomingRequestUseCase.respondWithSuccessSubintent(
                    request = data.value.request,
                    signedSubintent = signedSubintent
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isSubmitting = false,
                    error = TransactionErrorMessage(error)
                )
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private suspend fun handleSignAndSubmitFailure(error: Throwable) {
        logger.e(error)
        approvalJob = null

        when (error) {
            is ProfileException.SecureStorageAccess -> {
                appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                _state.update { it.copy(isSubmitting = false) }
            }

            // When rejected by user (signing with Ledger), we just need to stop the submit process.
            // No need to report back to dApp, as the user can retry and no need to show an error.
            is RadixWalletException.DappRequestException.RejectedByUser -> {
                _state.update { it.copy(isSubmitting = false) }
            }

            // These two kinds of errors should not report back to the dApp. The user can recover.
            // Although the error should appear.
            is RadixWalletException.LedgerCommunicationException,
            is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent -> {
                logNonFatalException(error)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = TransactionErrorMessage(error)
                    )
                }
            }

            // Errors that need to be reported both to the user and back to the dApp. The user cannot recover.
            // A fail event is fired.
            else -> {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = TransactionErrorMessage(error)
                    )
                }

                if (!data.value.request.isInternal) {
                    error.asRadixWalletException()?.let { radixWalletException ->
                        radixWalletException.toDappWalletInteractionErrorType()?.let { walletErrorType ->
                            respondToIncomingRequestUseCase.respondWithFailure(
                                request = data.value.request,
                                dappWalletInteractionErrorType = walletErrorType,
                                message = radixWalletException.getDappMessage()
                            )
                        }
                    }
                }

                appEventBus.sendEvent(
                    AppEvent.Status.Transaction.Fail(
                        requestId = data.value.request.interactionId,
                        transactionId = "",
                        isInternal = data.value.request.isInternal,
                        errorMessage = exceptionMessageProvider.throwableMessage(error),
                        blockUntilComplete = data.value.request.blockUntilComplete,
                        walletErrorType = error.toDappWalletInteractionErrorType(),
                        isMobileConnect = data.value.request.isMobileConnectRequest,
                        dAppName = _state.value.proposingDApp?.name
                    )
                )
            }
        }
    }

    @Throws(CommonException::class)
    private fun TransactionManifest.addAssertions(
        deposits: List<AccountWithTransferables>
    ): TransactionManifest {
        val allTransferables = deposits.map { it.transferables }.flatten()

        val guarantees = allTransferables.mapNotNull { transferable ->
            val amount = ((transferable as? Transferable.FungibleType)?.amount as? BoundedAmount.Predicted) ?: return@mapNotNull null
            val fungibleAsset = (transferable.asset as? Asset.Fungible) ?: return@mapNotNull null

            TransactionGuarantee(
                amount = amount.estimated,
                instructionIndex = amount.instructionIndex.toULong(),
                resourceAddress = fungibleAsset.resource.address,
                resourceDivisibility = fungibleAsset.resource.divisibility?.value,
                percentage = amount.offset
            )
        }
        return modifyAddGuarantees(guarantees = guarantees)
    }
}
