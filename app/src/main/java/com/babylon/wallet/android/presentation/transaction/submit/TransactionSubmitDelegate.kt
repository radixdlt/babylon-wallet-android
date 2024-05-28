package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.getDappMessage
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.IncomingMessage
import com.babylon.wallet.android.domain.model.Transferable
import com.babylon.wallet.android.domain.toConnectorExtensionError
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.domain.usecases.SignTransactionUseCase
import com.babylon.wallet.android.domain.usecases.transaction.SubmitTransactionUseCase
import com.babylon.wallet.android.presentation.common.ViewModelDelegate
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionErrorMessage
import com.babylon.wallet.android.presentation.transaction.TransactionReviewViewModel
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
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
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

    @Suppress("SwallowedException")
    fun onSubmit(
        signTransactionUseCase: SignTransactionUseCase,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = applicationScope.launch {
            val currentState = _state.value
            val currentNetworkId = getCurrentGatewayUseCase().network.id
            val manifestNetworkId = currentState.requestNonNull.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure =
                    RadixWalletException.DappRequestException.WrongNetwork(currentNetworkId, manifestNetworkId)
                onDismiss(
                    signTransactionUseCase = signTransactionUseCase,
                    exception = failure
                )
                return@launch
            }

            if (currentState.feePayers?.selectedAccountAddress != null) {
                val requestWithGuarantees = try {
                    val request = currentState.requestNonNull
                    request.copy(transactionManifestData = request.transactionManifestData.attachGuarantees(currentState.previewType))
                } catch (exception: Exception) {
                    return@launch reportFailure(RadixWalletException.PrepareTransactionException.ConvertManifest)
                }

                signAndSubmit(
                    transactionRequest = requestWithGuarantees,
                    signTransactionUseCase = signTransactionUseCase,
                    feePayerAddress = currentState.feePayers.selectedAccountAddress,
                    deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
                )
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
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = request,
                    error = exception.ceError,
                    message = exception.getDappMessage()
                )
            }
            _state.update {
                it.copy(isTransactionDismissed = true)
            }
            incomingRequestRepository.requestHandled(request.interactionId.toString())
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
        transactionRequest: IncomingMessage.IncomingRequest.TransactionRequest,
        signTransactionUseCase: SignTransactionUseCase,
        feePayerAddress: AccountAddress?,
        deviceBiometricAuthenticationProvider: suspend () -> Boolean
    ) {
        _state.update { it.copy(isSubmitting = true) }

        signTransactionUseCase.sign(
            request = SignTransactionUseCase.Request(
                manifest = transactionRequest.transactionManifestData,
                lockFee = _state.value.transactionFees.transactionFeeToLock,
                tipPercentage = _state.value.transactionFees.tipPercentageForTransaction,
                ephemeralNotaryPrivateKey = _state.value.ephemeralNotaryPrivateKey,
                feePayerAddress = feePayerAddress
            ),
            deviceBiometricAuthenticationProvider = deviceBiometricAuthenticationProvider
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
                    requestId = transactionRequest.interactionId.toString(),
                    transactionId = notarization.intentHash.bech32EncodedTxId,
                    isInternal = transactionRequest.isInternal,
                    blockUntilComplete = transactionRequest.blockUntilComplete
                )
            )
            transactionStatusClient.pollTransactionStatus(
                txID = notarization.intentHash.bech32EncodedTxId,
                requestId = transactionRequest.interactionId.toString(),
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
        }.onFailure { throwable ->
            throwable.asRadixWalletException()?.let { radixWalletException ->
                if (radixWalletException.cause is ProfileException.SecureStorageAccess) {
                    appEventBus.sendEvent(AppEvent.SecureFolderWarning)
                }
                when (radixWalletException) {
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
                                requestId = transactionRequest.interactionId.toString(),
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
                respondToIncomingRequestUseCase.respondWithFailure(
                    request = currentState.requestNonNull,
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
