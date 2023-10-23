package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.addAssertions
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.usecases.transaction.SignatureCancelledException
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.domain.toWalletErrorType
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.derivation.model.NetworkId
import rdx.works.profile.domain.NoMnemonicException
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
class TransactionSubmitDelegate @Inject constructor(
    private val transactionClient: TransactionClient,
    private val dAppMessenger: DappMessenger,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val submitTransactionUseCase: SubmitTransactionUseCase,
    private val appEventBus: AppEventBus,
    private val transactionStatusClient: TransactionStatusClient,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModelDelegate<TransactionReviewViewModel.State>() {

    private val logger = Timber.tag("TransactionSubmit")

    private var approvalJob: Job? = null

    fun onSubmit(deviceBiometricAuthenticationProvider: suspend () -> Boolean) {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = applicationScope.launch {
            val currentState = _state.value
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            val manifestNetworkId = currentState.requestNonNull.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = RadixWalletException.DappRequestException.WrongNetwork(currentNetworkId, manifestNetworkId)
                onDismiss(failure = failure)
                return@launch
            }

            _state.update { it.copy(isSubmitting = true) }

            currentState.requestNonNull.transactionManifestData.toTransactionManifest().onSuccess { manifest ->
                resolveFeePayerAndSubmit(
                    manifest.attachGuarantees(currentState.previewType),
                    deviceBiometricAuthenticationProvider
                )
            }.onFailure { error ->
                reportFailure(error)
            }
        }
    }

    suspend fun onDismiss(failure: RadixWalletException.DappRequestException) {
        if (approvalJob == null) {
            val request = _state.value.requestNonNull
            if (!request.isInternal) {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    remoteConnectorId = request.remoteConnectorId,
                    requestId = request.id,
                    error = failure.toWalletErrorType(),
                    message = failure.getDappMessage()
                )
            }
            _state.update {
                it.copy(isTransactionDismissed = true)
            }
            incomingRequestRepository.requestHandled(request.id)
        } else if (_state.value.interactionState != null) {
            approvalJob?.cancel()
            approvalJob = null
            transactionClient.cancelSigning()
            _state.update {
                it.copy(isSubmitting = false)
            }
        } else {
            logger.d("Cannot dismiss transaction while is in progress")
        }
    }

    private suspend fun resolveFeePayerAndSubmit(
        manifest: TransactionManifest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        _state.value.feePayerSearchResult?.let { feePayerResult ->
            _state.update { it.copy(isSubmitting = false) }
            if (feePayerResult.feePayerAddress != null) {
                signAndSubmit(
                    transactionRequest = _state.value.requestNonNull,
                    feePayerAddress = feePayerResult.feePayerAddress,
                    manifest = manifest,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
                )
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun signAndSubmit(
        transactionRequest: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        feePayerAddress: String,
        manifest: TransactionManifest,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        _state.update {
            it.copy(
                isSubmitting = true,
            )
        }
        val lockFee = _state.value.transactionFees.transactionFeeToLock
        val tipPercentage = _state.value.transactionFees.tipPercentageForTransaction
        val request = TransactionApprovalRequest(
            manifest = manifest,
            networkId = NetworkId.from(transactionRequest.requestMetadata.networkId),
            ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey,
            feePayerAddress = feePayerAddress,
            message = transactionRequest.transactionManifestData.message?.let {
                TransactionApprovalRequest.TransactionMessage.Public(it)
            } ?: TransactionApprovalRequest.TransactionMessage.None
        )

        transactionClient.signTransaction(
            request = request,
            lockFee = lockFee,
            tipPercentage = tipPercentage,
            deviceBiometricAuthenticationProvider
        ).mapCatching { notarizedTransactionResult ->
            submitTransactionUseCase(
                notarizedTransactionResult.txIdHash,
                notarizedTransactionResult.notarizedTransactionIntentHex,
                txProcessingTime = notarizedTransactionResult.txProcessingTime
            ).getOrThrow()
        }.onSuccess { submitTransactionResult ->
            _state.update {
                it.copy(
                    isSubmitting = false,
                    txProcessingTime = submitTransactionResult.txProcessingTime
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
                txProcessingTime = submitTransactionResult.txProcessingTime
            )
            // Send confirmation to the dApp that tx was submitted before status polling
            if (!transactionRequest.isInternal) {
                dAppMessenger.sendTransactionWriteResponseSuccess(
                    remoteConnectorId = transactionRequest.remoteConnectorId,
                    requestId = transactionRequest.requestId,
                    txId = submitTransactionResult.txId
                )
            }
        }.onFailure { error ->
            // we always wrap exception returned from signing/submitting flow in DappRequestException
//            (error as? DappRequestException)?.let { dappRequestException ->
//                val cancelled = dappRequestException.e is SignatureCancelledException
//                val failedToSign =
//                    dappRequestException.failure is DappRequestFailure.TransactionApprovalFailure.SignCompiledTransactionIntent
//                val failedToCollectLedgerSignature = dappRequestException.failure is DappRequestFailure.LedgerCommunicationFailure
//                when {
//                    cancelled || failedToCollectLedgerSignature -> {
//                        state.update { it.copy(isSubmitting = false) }
//                        approvalJob = null
//                    }
//
//                    failedToSign -> {
//                        val noMnemonicException = dappRequestException.e is NoMnemonicException
//                        if (noMnemonicException) {
//                            state.update {
//                                it.copy(isNoMnemonicErrorVisible = true, isSubmitting = false)
//                            }
//                        } else {
//                            state.update {
//                                it.copy(isSubmitting = false, error = UiMessage.ErrorMessage.from(dappRequestException.failure))
//                            }
//                        }
//                        approvalJob = null
//                    }
//
//                    else -> {
//                        reportFailure(error)
//                        appEventBus.sendEvent(
//                            AppEvent.Status.Transaction.Fail(
//                                requestId = transactionRequest.requestId,
//                                transactionId = "",
//                                isInternal = transactionRequest.isInternal,
//                                errorMessage = UiMessage.ErrorMessage.from((error as? DappRequestException)?.failure),
//                                blockUntilComplete = transactionRequest.blockUntilComplete
//                            )
//                        )
//                    }
//                }
//            }
            (error as? RadixWalletException)?.let { radixWalletException ->
                when (radixWalletException) {
                    is RadixWalletException.SignatureCancelledException,
                    is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent,
                    is RadixWalletException.LedgerCommunicationFailure -> {
                        val failedToSign =
                            radixWalletException is RadixWalletException.PrepareTransactionException.SignCompiledTransactionIntent
                        state.update {
                            it.copy(
                                isSubmitting = false,
                                error = if (failedToSign) {
                                    UiMessage.ErrorMessage(radixWalletException)
                                } else {
                                    it.error
                                }
                            )
                        }
                        approvalJob = null
                        return
                    }

                    else -> {
                        reportFailure(error)
                        appEventBus.sendEvent(
                            AppEvent.Status.Transaction.Fail(
                                requestId = transactionRequest.requestId,
                                transactionId = "",
                                isInternal = transactionRequest.isInternal,
                                errorMessage = UiMessage.ErrorMessage.from((error as? DappRequestException)?.failure),
                                blockUntilComplete = transactionRequest.blockUntilComplete,
                                txProcessingTime = _state.value.txProcessingTime,
                                walletErrorType = walletErrorType
                            )
                        )
                    }
                    }
                }
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
        logger.w(error)
        _state.update {
            it.copy(isSubmitting = false, error = UiMessage.ErrorMessage(error))
        }

        val currentState = state.value
        if (currentState.requestNonNull.isInternal) {
            return
        }
        //TODO test this flow
        if (error is RadixWalletException) {
            error.toWalletErrorType()?.let { walletErrorType ->
                dAppMessenger.sendWalletInteractionResponseFailure(
                    remoteConnectorId = currentState.requestNonNull.remoteConnectorId,
                    requestId = currentState.requestNonNull.requestId,
                    error = walletErrorType,
                    message = error.getDappMessage()
                )
            }
        }
        approvalJob = null
    }
}
