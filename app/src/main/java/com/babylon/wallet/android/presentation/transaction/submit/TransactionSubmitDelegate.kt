package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
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
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.Event
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.presentation.transaction.model.TransactionErrorMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.TransactionGuarantee
import com.radixdlt.sargon.extensions.modifyAddGuarantees
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.domain.TransactionManifestData
import rdx.works.core.domain.resources.Resource
import rdx.works.core.logNonFatalException
import rdx.works.core.then
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionSubmitDelegate @Inject constructor(
    private val signTransactionUseCase: SignTransactionUseCase,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val submitTransactionUseCase: SubmitTransactionUseCase,
    private val clearCachedNewlyCreatedEntitiesUseCase: ClearCachedNewlyCreatedEntitiesUseCase,
    private val appEventBus: AppEventBus,
    private val transactionStatusClient: TransactionStatusClient,
    private val exceptionMessageProvider: ExceptionMessageProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    var oneOffEventHandler: OneOffEventHandler<Event>? = null

    fun onSubmit() {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = applicationScope.launch {
            val currentState = _state.value
            val currentNetworkId = getCurrentGatewayUseCase().network.id
            val manifestNetworkId = currentState.requestNonNull.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = RadixWalletException.DappRequestException.WrongNetwork(
                    currentNetworkId = currentNetworkId,
                    requestNetworkId = manifestNetworkId
                )
                onDismiss(exception = failure)
                return@launch
            }

            if (currentState.feePayers?.selectedAccountAddress != null) {
                val requestWithGuarantees = try {
                    val request = currentState.requestNonNull
                    request.copy(transactionManifestData = request.transactionManifestData.attachGuarantees(currentState.previewType))
                } catch (exception: Exception) {
                    logger.e(exception)
                    return@launch reportFailure(RadixWalletException.PrepareTransactionException.ConvertManifest)
                }

                signAndSubmit(
                    transactionRequest = requestWithGuarantees,
                    signTransactionUseCase = signTransactionUseCase,
                    feePayerAddress = currentState.feePayers.selectedAccountAddress
                )
            }
        }
    }

    suspend fun onDismiss(exception: RadixWalletException.DappRequestException): Result<Unit> = runCatching {
        if (approvalJob == null) {
            val request = _state.value.requestNonNull
            if (!request.isInternal) {
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = request,
                    dappWalletInteractionErrorType = exception.dappWalletInteractionErrorType,
                    message = exception.getDappMessage()
                )
            }
            oneOffEventHandler?.sendEvent(Event.Dismiss)
            incomingRequestRepository.requestHandled(request.interactionId)
        } else {
            logger.d("Cannot dismiss transaction while is in progress")
        }
    }

    private suspend fun signAndSubmit(
        transactionRequest: TransactionRequest,
        signTransactionUseCase: SignTransactionUseCase,
        feePayerAddress: AccountAddress?
    ) {
        _state.update { it.copy(isSubmitting = true) }

        signTransactionUseCase(
            request = SignTransactionUseCase.Request(
                manifest = transactionRequest.transactionManifestData,
                lockFee = _state.value.transactionFees.transactionFeeToLock,
                tipPercentage = _state.value.transactionFees.tipPercentageForTransaction,
                ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey,
                feePayerAddress = feePayerAddress
            )
        ).then { notarizationResult ->
            submitTransactionUseCase(notarizationResult = notarizationResult)
        }.onSuccess { notarization ->
            _state.update {
                it.copy(
                    isSubmitting = false,
                    endEpoch = notarization.endEpoch
                )
            }
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
                txID = notarization.intentHash.bech32EncodedTxId,
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

    private fun TransactionManifestData.attachGuarantees(previewType: PreviewType): TransactionManifestData {
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

        val currentState = _state.value
        if (currentState.requestNonNull.isInternal) {
            return
        }
        error.asRadixWalletException()?.let { radixWalletException ->
            radixWalletException.toDappWalletInteractionErrorType()?.let { walletErrorType ->
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = currentState.requestNonNull,
                    dappWalletInteractionErrorType = walletErrorType,
                    message = radixWalletException.getDappMessage()
                )
            }
        }
        approvalJob = null
    }

    private fun TransactionManifestData.addAssertions(
        depositing: List<Transferable.Depositing>
    ): TransactionManifestData {
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

        return TransactionManifestData.from(
            manifest = manifestSargon.modifyAddGuarantees(guarantees = guarantees),
            message = message
        )
    }
}
