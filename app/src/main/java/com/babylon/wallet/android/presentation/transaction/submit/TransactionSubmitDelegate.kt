package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.toConnectorExtensionError
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.logNonFatalException
import rdx.works.core.then
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import rdx.works.profile.ret.addGuaranteeInstructionToManifest
import rdx.works.profile.ret.transaction.TransactionManifestData
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionSubmitDelegate @Inject constructor(
    private val dAppMessenger: DappMessenger,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val submitTransactionUseCase: SubmitTransactionUseCase,
    private val appEventBus: AppEventBus,
    private val transactionStatusClient: TransactionStatusClient,
    private val exceptionMessageProvider: ExceptionMessageProvider,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    fun onSubmit(
        signTransactionUseCase: SignTransactionUseCase,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = applicationScope.launch {
            val currentState = _state.value
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            val manifestNetworkId = currentState.requestNonNull.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = RadixWalletException.DappRequestException.WrongNetwork(currentNetworkId, manifestNetworkId)
                onDismiss(
                    signTransactionUseCase = signTransactionUseCase,
                    exception = failure
                )
                return@launch
            }

            _state.update { it.copy(isSubmitting = true) }

            val request = _state.value.requestNonNull
            val requestWithGuarantees = request.copy(
                transactionManifestData = request.transactionManifestData.attachGuarantees(currentState.previewType)
            )
            _state.value.feePayerSearchResult?.let { feePayerResult ->
                _state.update { it.copy(isSubmitting = false) }

                if (feePayerResult.feePayerAddress != null) {
                    signAndSubmit(
                        transactionRequest = requestWithGuarantees,
                        signTransactionUseCase = signTransactionUseCase,
                        feePayerAddress = feePayerResult.feePayerAddress,
                        deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
                    )
                }
            }
        }
    }

    suspend fun onDismiss(
        signTransactionUseCase: SignTransactionUseCase,
        exception: RadixWalletException.DappRequestException
    ) {
        if (approvalJob == null) {
            val request = _state.value.requestNonNull
            if (!request.isInternal) {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    remoteConnectorId = request.remoteConnectorId,
                    requestId = request.id,
                    error = exception.ceError,
                    message = exception.getDappMessage()
                )
            }
            _state.update {
                it.copy(isTransactionDismissed = true)
            }
            incomingRequestRepository.requestHandled(request.id)
        } else if (_state.value.interactionState != null) {
            approvalJob?.cancel()
            approvalJob = null
            signTransactionUseCase.cancelSigning()
            _state.update {
                it.copy(isSubmitting = false)
            }
        } else {
            logger.d("Cannot dismiss transaction while is in progress")
        }
    }

    @Suppress("LongMethod")
    private suspend fun signAndSubmit(
        transactionRequest: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        signTransactionUseCase: SignTransactionUseCase,
        feePayerAddress: String,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        _state.update {
            it.copy(
                isSubmitting = true,
            )
        }

        signTransactionUseCase.sign(
            request = SignTransactionUseCase.Request(
                manifest = transactionRequest.transactionManifestData,
                lockFee = _state.value.transactionFees.transactionFeeToLock,
                tipPercentage = _state.value.transactionFees.tipPercentageForTransaction,
                ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey,
                feePayerAddress = feePayerAddress
            ),
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
        ).then { notarisation ->
            submitTransactionUseCase(
                txIDHash = notarisation.txIdHash,
                notarizedTransactionHex = notarisation.notarizedTransactionIntentHex,
                endEpoch = notarisation.endEpoch
            )
        }.onSuccess { submitTransactionResult ->
            _state.update {
                it.copy(
                    isSubmitting = false,
                    endEpoch = submitTransactionResult.endEpoch
                )
            }
            appEventBus.sendEvent(
                AppEvent.Status.Transaction.InProgress(
                    requestId = transactionRequest.requestId,
                    transactionId = submitTransactionResult.txId,
                    isInternal = transactionRequest.isInternal,
                    blockUntilComplete = transactionRequest.blockUntilComplete
                )
            )
            transactionStatusClient.pollTransactionStatus(
                txID = submitTransactionResult.txId,
                requestId = transactionRequest.requestId,
                transactionType = transactionRequest.transactionType,
                endEpoch = submitTransactionResult.endEpoch
            )
            // Send confirmation to the dApp that tx was submitted before status polling
            if (!transactionRequest.isInternal) {
                dAppMessenger.sendTransactionWriteResponseSuccess(
                    remoteConnectorId = transactionRequest.remoteConnectorId,
                    requestId = transactionRequest.requestId,
                    txId = submitTransactionResult.txId
                )
            }
        }.onFailure { throwable ->
            throwable.asRadixWalletException()?.let { radixWalletException ->
                when (radixWalletException) {
                    is RadixWalletException.SignatureCancelled,
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
                    is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent -> {
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
                                requestId = transactionRequest.requestId,
                                transactionId = "",
                                isInternal = transactionRequest.isInternal,
                                errorMessage = exceptionMessageProvider.throwableMessage(radixWalletException),
                                blockUntilComplete = transactionRequest.blockUntilComplete,
                                walletErrorType = radixWalletException.toConnectorExtensionError()
                            )
                        )
                    }
                }
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
            radixWalletException.toConnectorExtensionError()?.let { walletErrorType ->
                dAppMessenger.sendWalletInteractionResponseFailure(
                    remoteConnectorId = currentState.requestNonNull.remoteConnectorId,
                    requestId = currentState.requestNonNull.requestId,
                    error = walletErrorType,
                    message = radixWalletException.getDappMessage()
                )
            }
        }
        approvalJob = null
    }

    private fun TransactionManifestData.addAssertions(
        depositing: List<Transferable.Depositing>
    ): TransactionManifestData {
        var startIndex = 0
        var manifest = this

        depositing.forEach {
            when (val assertion = it.guaranteeAssertion) {
                is GuaranteeAssertion.ForAmount -> {
                    manifest = manifest.addGuaranteeInstructionToManifest(
                        address = it.transferable.resourceAddress,
                        guaranteedAmount = assertion.amount,
                        index = assertion.instructionIndex.toInt() + startIndex
                    )
                    startIndex++
                }

                is GuaranteeAssertion.ForNFT -> {
                    // Will be implemented later
                }

                null -> {}
            }
        }

        return manifest
    }
}
