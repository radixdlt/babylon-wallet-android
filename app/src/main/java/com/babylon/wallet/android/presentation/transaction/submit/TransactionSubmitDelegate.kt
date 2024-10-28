package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.repository.transaction.TransactionRepository
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.model.messages.TransactionRequest
import com.babylon.wallet.android.domain.toDappWalletInteractionErrorType
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.assets.ClearCachedNewlyCreatedEntitiesUseCase
import com.babylon.wallet.android.domain.usecases.signing.SignTransactionUseCase
import com.babylon.wallet.android.presentation.common.DataHolderViewModelDelegate
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.TransactionGuarantee
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.modifyAddGuarantees
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.resources.Resource
import rdx.works.core.logNonFatalException
import rdx.works.core.then
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import javax.inject.Inject

interface TransactionSubmitDelegate {

    fun onApproveTransaction()
}

@Suppress("LongParameterList")
class TransactionSubmitDelegateImpl @Inject constructor(
    private val signTransactionUseCase: SignTransactionUseCase,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionRepository: TransactionRepository,
    private val clearCachedNewlyCreatedEntitiesUseCase: ClearCachedNewlyCreatedEntitiesUseCase,
    private val appEventBus: AppEventBus,
    private val transactionStatusClient: TransactionStatusClient,
    private val exceptionMessageProvider: ExceptionMessageProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
) : DataHolderViewModelDelegate<TransactionReviewViewModel.Data, TransactionReviewViewModel.State>(),
    TransactionSubmitDelegate {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    var oneOffEventHandler: OneOffEventHandler<TransactionReviewViewModel.Event>? = null

    override fun onApproveTransaction() {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return
        val txToReviewData = data.value.transactionToReviewData

        approvalJob = applicationScope.launch {
            val currentNetworkId = getCurrentGatewayUseCase().network.id
            val manifestNetworkId = txToReviewData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = RadixWalletException.DappRequestException.WrongNetwork(
                    currentNetworkId = currentNetworkId,
                    requestNetworkId = manifestNetworkId
                )
                onDismiss(exception = failure)
                return@launch
            }

            try {
                val newManifest = txToReviewData.transactionToReview.transactionManifest.attachGuarantees(_state.value.previewType)
                data.update {
                    it.copy(
                        txToReviewData = it.transactionToReviewData.copy(
                            transactionToReview = it.transactionToReviewData.transactionToReview.copy(
                                transactionManifest = newManifest
                            )
                        )
                    )
                }
            } catch (exception: Exception) {
                logger.e(exception)
                return@launch reportFailure(RadixWalletException.PrepareTransactionException.ConvertManifest)
            }

            signAndSubmit()
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

    private suspend fun signAndSubmit() {
        val transactionRequest = data.value.request
        val feePayerAddress = data.value.feePayers?.selectedAccountAddress

        _state.update { it.copy(isSubmitting = true) }

        signTransactionUseCase(
            request = SignTransactionUseCase.Request(
                manifest = data.value.transactionToReviewData.transactionToReview.transactionManifest,
                networkId = data.value.transactionToReviewData.networkId,
                message = data.value.transactionToReviewData.message,
                lockFee = _state.value.transactionFees.transactionFeeToLock,
                tipPercentage = _state.value.transactionFees.tipPercentageForTransaction,
                ephemeralNotaryPrivateKey = data.value.ephemeralNotaryPrivateKey,
                feePayerAddress = feePayerAddress
            )
        ).then { notarizationResult ->
            transactionRepository.submitTransaction(notarizationResult.notarizedTransaction)
                .map { notarizationResult }
        }.onSuccess { notarization ->
            data.update { it.copy(endEpoch = notarization.endEpoch) }
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
            transactionStatusClient.pollTransactionStatus(
                intentHash = notarization.intentHash,
                requestId = transactionRequest.interactionId,
                transactionType = transactionRequest.transactionType,
                endEpoch = notarization.endEpoch
            )
            // Send confirmation to the dApp that tx was submitted before status polling
            if (!transactionRequest.isInternal) {
                respondToIncomingRequestUseCase.respondWithSuccess(
                    request = transactionRequest,
                    txId = notarization.intentHash.bech32EncodedTxId
                )
            }
            val previewType = _state.value.previewType
            if (previewType is PreviewType.Transfer) {
                clearCachedNewlyCreatedEntitiesUseCase(previewType.newlyCreatedNFTItemsForExistingResources)
            }
        }.onFailure { throwable ->
            throwable.asRadixWalletException()?.let { radixWalletException ->
                handleSubmitFailure(transactionRequest, radixWalletException)
            }
        }
    }

    private suspend fun handleSubmitFailure(
        transactionRequest: TransactionRequest,
        radixWalletException: RadixWalletException
    ) {
        if (radixWalletException.cause is ProfileException.SecureStorageAccess) {
            appEventBus.sendEvent(AppEvent.SecureFolderWarning)
        }
        when (radixWalletException) {
            // if signing rejected by user do not show any error dialog
            is RadixWalletException.DappRequestException.RejectedByUser -> {
                _state.update { it.copy(isSubmitting = false) }
                approvalJob = null
                return
            }

            is RadixWalletException.SignatureCancelled,
            is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent,
            is RadixWalletException.LedgerCommunicationException -> {
                logNonFatalException(radixWalletException)
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        error = TransactionErrorMessage(radixWalletException)
                    )
                }
                approvalJob = null
                return
            }

            else -> {
                reportFailure(radixWalletException)
                appEventBus.sendEvent(
                    AppEvent.Status.Transaction.Fail(
                        requestId = transactionRequest.interactionId,
                        transactionId = "",
                        isInternal = transactionRequest.isInternal,
                        errorMessage = exceptionMessageProvider.throwableMessage(radixWalletException),
                        blockUntilComplete = transactionRequest.blockUntilComplete,
                        walletErrorType = radixWalletException.toDappWalletInteractionErrorType(),
                        isMobileConnect = transactionRequest.isMobileConnectRequest,
                        dAppName = _state.value.proposingDApp?.name
                    )
                )
            }
        }
    }

    private fun TransactionManifest.attachGuarantees(previewType: PreviewType): TransactionManifest {
        var manifest = this
        if (previewType is PreviewType.Transfer) {
            manifest = manifest.addAssertions(
                depositing = previewType.to.map {
                    it.resources
                }.flatten().filterIsInstance<Transferable.Depositing>()
            )
        }

        return manifest
    }

    private suspend fun reportFailure(error: Throwable) {
        logNonFatalException(error)
        logger.w(error)
        _state.update {
            it.copy(isSubmitting = false, error = TransactionErrorMessage(error))
        }

        if (data.value.request.isInternal) {
            return
        }
        error.asRadixWalletException()?.let { radixWalletException ->
            radixWalletException.toDappWalletInteractionErrorType()?.let { walletErrorType ->
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = data.value.request,
                    dappWalletInteractionErrorType = walletErrorType,
                    message = radixWalletException.getDappMessage()
                )
            }
        }
        approvalJob = null
    }

    private fun TransactionManifest.addAssertions(
        depositing: List<Transferable.Depositing>
    ): TransactionManifest {
        val guarantees = depositing.mapNotNull { transferable ->
            val assertion = transferable.guaranteeAssertion as? GuaranteeAssertion.ForAmount ?: return@mapNotNull null
            val resource = transferable.transferable.resource as? Resource.FungibleResource ?: return@mapNotNull null
            TransactionGuarantee(
                amount = assertion.amount,
                instructionIndex = assertion.instructionIndex.toULong(),
                resourceAddress = resource.address,
                resourceDivisibility = resource.divisibility?.value,
                percentage = assertion.percentage
            )
        }
        return modifyAddGuarantees(guarantees = guarantees)
    }
}
