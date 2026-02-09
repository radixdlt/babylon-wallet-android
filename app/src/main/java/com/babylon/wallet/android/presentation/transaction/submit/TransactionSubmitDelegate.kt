package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
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
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel.State.Sheet
import com.babylon.wallet.android.presentation.transaction.analysis.summary.SummarizedManifest
import com.babylon.wallet.android.presentation.transaction.analysis.summary.Summary
import com.babylon.wallet.android.presentation.transaction.model.AccountWithTransferables
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.model.Transferable
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.CommonException
import com.radixdlt.sargon.ExecutionSummary
import com.radixdlt.sargon.SubintentManifest
import com.radixdlt.sargon.TransactionGuarantee
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.hash
import com.radixdlt.sargon.extensions.then
import com.radixdlt.sargon.isAccessControllerTimedRecoveryManifest
import com.radixdlt.sargon.os.SargonOsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.assets.Asset
import rdx.works.core.mapError
import rdx.works.core.sargon.timeUntilDelayedConfirmationIsCallable
import rdx.works.core.toUnitResult
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

interface TransactionSubmitDelegate {

    fun onSignAndSubmitTransaction()

    fun onRestartSignAndSubmitTransaction()

    fun onSigningCanceled()
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
    private val sargonOsManager: SargonOsManager
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>(),
    TransactionSubmitDelegate {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    var oneOffEventHandler: OneOffEventHandler<TransactionReviewViewModel.Event>? = null
    private val timedRecoveryConfirmedChannel = Channel<Boolean>()

    override fun onSignAndSubmitTransaction() {
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

            prepareSummary().then { summary ->
                signAndSubmit(summary = summary)
            }.onSuccess {
                approvalJob = null

                val previewType = _state.value.previewType as? PreviewType.Transaction ?: return@onSuccess
                clearCachedNewlyCreatedEntitiesUseCase(previewType.newlyCreatedNFTs)
            }.onFailure { error ->
                logger.e(error)
                approvalJob = null

                when (error) {
                    // When signing is rejected we just need to stop the submit process. User can retry.
                    is CommonException.HostInteractionAborted -> {
                        _state.update { it.copy(isSubmitting = false) }
                    }

                    // When signing failed due to many factor sources were skipped, the user must see the appropriate modal
                    is CommonException.SigningFailedTooManyFactorSourcesNeglected -> {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                sheetState = Sheet.SigningFailed.from(isPreAuthorization = data.value.request.kind.isPreAuthorized)
                            )
                        }
                    }

                    // The rest of the errors are reported to the user
                    else -> {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                error = TransactionErrorMessage(error)
                            )
                        }

                        // In case of a regular transaction, the error should be sent to the dApp and the flow should complete with failure.
                        if (!data.value.request.kind.isPreAuthorized) {
                            handleOtherTransactionFailure(error)
                        }
                    }
                }
            }
        }
    }

    override fun onRestartSignAndSubmitTransaction() {
        _state.update { it.copy(sheetState = Sheet.None) }
        onSignAndSubmitTransaction()
    }

    override fun onSigningCanceled() {
        _state.update { it.copy(sheetState = Sheet.None) }
        viewModelScope.launch {
            onDismiss(exception = RadixWalletException.DappRequestException.RejectedByUser)
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

    @Suppress("MagicNumber")
    fun onRestartSigningFromTimedRecoverySheet() {
        viewModelScope.launch {
            onConfirmTimedRecoverySheetDismiss(false)
            delay(300L) // Allow time for the previous signing flow to fully dismiss
            onRestartSignAndSubmitTransaction()
        }
    }

    fun onConfirmTimedRecoverySheetDismiss(confirmed: Boolean) {
        _state.update { state ->
            state.copy(sheetState = Sheet.None)
        }

        viewModelScope.launch {
            timedRecoveryConfirmedChannel.send(confirmed)
        }
    }

    private suspend fun prepareSummary(): Result<Summary> = runCatching {
        when (val summary = data.value.summary) {
            is Summary.FromExecution -> {
                val transactionPreviewType = (_state.value.previewType as? PreviewType.Transaction)
                val transactionManifest = (summary.manifest as? SummarizedManifest.Transaction)?.manifest
                if (transactionPreviewType != null && transactionManifest != null) {
                    summary.copy(
                        manifest = SummarizedManifest.Transaction(
                            manifest = transactionManifest.addAssertions(transactionPreviewType.to)
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
                is SummarizedManifest.Transaction -> signAndSubmit(
                    transactionManifest = summary.manifest.manifest,
                    executionSummary = summary.summary
                )
            }

            is Summary.FromStaticAnalysis -> signAndSubmit(subintentManifest = summary.manifest.manifest)
        }
    }

    private suspend fun signAndSubmit(
        transactionManifest: TransactionManifest,
        executionSummary: ExecutionSummary
    ): Result<Unit> {
        val fees = _state.value.fees ?: error("Fees were not resolved")
        val transactionRequest = data.value.request
        val transactionRequestKind = transactionRequest.kind as? TransactionRequest.Kind.Regular
            ?: error("Wrong kind: ${transactionRequest.kind}")

        return signAndNotarizeTransactionUseCase(
            manifest = transactionManifest,
            executionSummary = executionSummary,
            networkId = transactionRequest.unvalidatedManifestData.networkId,
            message = transactionRequest.unvalidatedManifestData.message,
            lockFee = fees.transactionFees.transactionFeeToLock,
            tipPercentage = fees.transactionFees.tipPercentageForTransaction,
            notarySecretKey = data.value.ephemeralNotaryPrivateKey,
            feePayerAddress = data.value.feePayers?.selectedAccountAddress,
            guarantees = buildGuarantees(
                deposits = (_state.value.previewType as? PreviewType.Transaction)?.to.orEmpty()
            )
        ).then { notarizationResult ->
            val signedManifest = notarizationResult.notarizedTransaction.signedIntent.intent.manifest

            val timedRecoveryConfirmed = if (isAccessControllerTimedRecoveryManifest(signedManifest)) {
                _state.update {
                    it.copy(
                        sheetState = Sheet.ConfirmTimedRecovery(
                            time = (_state.value.previewType as? PreviewType.UpdateSecurityStructure)
                                ?.entity?.securityState?.timeUntilDelayedConfirmationIsCallable
                        )
                    )
                }
                timedRecoveryConfirmedChannel.receive()
            } else {
                true
            }

            if (timedRecoveryConfirmed) {
                transactionRepository.submitTransaction(notarizationResult.notarizedTransaction)
                    .map { notarizationResult }
            } else {
                Result.failure(CommonException.HostInteractionAborted())
            }
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

            transactionStatusClient.observeTransactionStatus(
                intentHash = notarization.intentHash,
                requestId = data.value.request.interactionId,
                transactionType = transactionRequestKind.transactionType,
                endEpoch = notarization.endEpoch,
                signedManifest = notarization.notarizedTransaction.signedIntent.intent.manifest
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

    private suspend fun signAndSubmit(subintentManifest: SubintentManifest): Result<Unit> {
        val transactionRequest = data.value.request
        val transactionRequestKind = transactionRequest.kind as? TransactionRequest.Kind.PreAuthorized
            ?: error("Wrong kind: ${transactionRequest.kind}")

        return signSubintentUseCase(
            manifest = subintentManifest,
            message = transactionRequest.unvalidatedManifestData.plainMessage,
            expiration = transactionRequestKind.expiration,
            header = transactionRequestKind.header
        ).mapCatching { signedSubintent ->
            // Respond to dApp or throw an error if it fails, preventing the transaction status polling
            val expiration = respondToIncomingRequestUseCase.respondWithSuccessSubintent(
                request = data.value.request,
                signedSubintent = signedSubintent
            ).getOrThrow()

            appEventBus.sendEvent(
                AppEvent.Status.PreAuthorization.Sent(
                    requestId = transactionRequest.interactionId,
                    preAuthorizationId = signedSubintent.subintent.hash(),
                    isMobileConnect = transactionRequest.isMobileConnectRequest,
                    dAppName = _state.value.proposingDApp?.name,
                    remainingTime = (expiration.secondsSinceUnixEpoch - Instant.now().epochSecond).seconds
                )
            )

            transactionStatusClient.observePreAuthorizationStatus(
                intentHash = signedSubintent.subintent.hash(),
                requestId = data.value.request.interactionId,
                expiration = expiration
            )

            _state.update { state ->
                state.copy(isSubmitting = false)
            }
        }
    }

    private suspend fun handleOtherTransactionFailure(error: Throwable) {
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

    @Throws(CommonException::class)
    private suspend fun TransactionManifest.addAssertions(
        deposits: List<AccountWithTransferables>
    ): TransactionManifest {
        return sargonOsManager.sargonOs.modifyTransactionManifestWithoutFeePayer(
            transactionManifest = this,
            guarantees = buildGuarantees(deposits)
        )
    }

    private fun buildGuarantees(
        deposits: List<AccountWithTransferables>
    ): List<TransactionGuarantee> {
        val allTransferables = deposits.map { it.transferables }.flatten()

        return allTransferables.mapNotNull { transferable ->
            val amount = ((transferable as? Transferable.FungibleType)?.amount as? BoundedAmount.Predicted)
                ?: return@mapNotNull null
            val fungibleAsset = (transferable.asset as? Asset.Fungible) ?: return@mapNotNull null

            TransactionGuarantee(
                amount = amount.guaranteed,
                instructionIndex = amount.instructionIndex.toULong(),
                resourceAddress = fungibleAsset.resource.address,
                resourceDivisibility = fungibleAsset.resource.divisibility?.value,
                percentage = amount.offset
            )
        }
    }
}
