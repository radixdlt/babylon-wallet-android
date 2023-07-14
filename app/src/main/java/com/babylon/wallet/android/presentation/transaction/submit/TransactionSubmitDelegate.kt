package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.manifest.addGuaranteeInstructionToManifest
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.model.GuaranteeAssertion
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.PreviewType
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.Event
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel.State
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber

@Suppress("LongParameterList")
class TransactionSubmitDelegate(
    private val state: MutableStateFlow<State>,
    private val transactionClient: TransactionClient,
    private val dAppMessenger: DappMessenger,
    private val getCurrentGatewayUseCase: GetCurrentGatewayUseCase,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
    private val onSendScreenEvent: (Event) -> Unit
) {
    private var approvalJob: Job? = null

    fun onSubmit() {
        // Do not re-submit while submission is in progress
        if (approvalJob != null) return

        approvalJob = appScope.launch {
            val currentState = state.value
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            val manifestNetworkId = currentState.request.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = DappRequestFailure.WrongNetwork(currentNetworkId, manifestNetworkId)
                onDismiss(failure = failure)
                return@launch
            }

            state.update { it.copy(isSubmitting = true) }

            val manifest = currentState.request.transactionManifestData
                .toTransactionManifest()
                .attachGuarantees(currentState.previewType)

            transactionClient.findFeePayerInManifest(manifest)
                .onSuccess { feePayerResult ->
                    state.update { it.copy(isSubmitting = false) }
                    if (feePayerResult.feePayerAddressFromManifest != null) {
                        signAndSubmit(
                            transactionRequest = currentState.request,
                            feePayerAddress = feePayerResult.feePayerAddressFromManifest,
                            manifest = manifest
                        )
                    } else {
                        state.update { state ->
                            state.copy(
                                isSubmitting = false,
                                sheetState = State.Sheet.FeePayerChooser(
                                    candidates = feePayerResult.candidates,
                                    pendingManifest = manifest
                                )
                            )
                        }
                    }
                    approvalJob = null
                }
                .onFailure { error ->
                    state.update {
                        it.copy(isSubmitting = false, error = UiMessage.ErrorMessage.from(error = error))
                    }

                    if (error is DappRequestException && !currentState.request.isInternal) {
                        dAppMessenger.sendWalletInteractionResponseFailure(
                            dappId = currentState.request.dappId,
                            requestId = currentState.request.requestId,
                            error = error.failure.toWalletErrorType(),
                            message = error.failure.getDappMessage()
                        )
                    }

                    approvalJob = null
                }
        }
    }

    fun onFeePayerSelected(account: Network.Account) {
        val feePayerSheet = state.value.sheetState as? State.Sheet.FeePayerChooser ?: return
        state.update {
            it.copy(
                sheetState = feePayerSheet.copy(selectedCandidate = account)
            )
        }
    }

    fun onFeePayerConfirmed() {
        val feePayerSheet = state.value.sheetState as? State.Sheet.FeePayerChooser ?: return
        val selectedCandidate = feePayerSheet.selectedCandidate ?: return

        approvalJob = appScope.launch {
            state.update { it.copy(sheetState = State.Sheet.None) }
            signAndSubmit(state.value.request, selectedCandidate.address, feePayerSheet.pendingManifest)
        }
    }

    suspend fun onDismiss(failure: DappRequestFailure) {
        if (approvalJob == null) {
            val request = state.value.request
            if (!request.isInternal) {
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = request.dappId,
                    requestId = request.id,
                    error = failure.toWalletErrorType(),
                    message = failure.getDappMessage()
                )
            }
            onSendScreenEvent(Event.Dismiss)
            incomingRequestRepository.requestHandled(request.id)
        } else if (state.value.signingState != null) {
            approvalJob?.cancel()
            approvalJob = null
            transactionClient.cancelSigning()
            state.update {
                it.copy(isSubmitting = false)
            }
        } else {
            Timber.d("Cannot dismiss transaction while is in progress")
        }
    }

    @Suppress("LongMethod")
    private suspend fun signAndSubmit(
        transactionRequest: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        feePayerAddress: String,
        manifest: TransactionManifest
    ) {
        state.update {
            it.copy(
                isSubmitting = true,
            )
        }
        val request = TransactionApprovalRequest(
            manifest = manifest,
            ephemeralNotaryPrivateKey = state.value.ephemeralNotaryPrivateKey,
            feePayerAddress = feePayerAddress
        )
        transactionClient.signAndSubmitTransaction(request).onSuccess { txId ->
            state.update {
                it.copy(
                    isSubmitting = false
                )
            }
            appEventBus.sendEvent(
                AppEvent.Status.Transaction.InProgress(
                    requestId = transactionRequest.requestId,
                    transactionId = txId,
                    isInternal = transactionRequest.isInternal
                )
            )
            // Send confirmation to the dApp that tx was submitted before status polling
            if (!transactionRequest.isInternal) {
                dAppMessenger.sendTransactionWriteResponseSuccess(
                    dappId = transactionRequest.dappId,
                    requestId = transactionRequest.requestId,
                    txId = txId
                )
            }

            appEventBus.sendEvent(
                AppEvent.Status.Transaction.InProgress(
                    requestId = transactionRequest.requestId,
                    transactionId = txId,
                    isInternal = transactionRequest.isInternal
                )
            )
        }.onFailure { error ->
            state.update {
                it.copy(
                    isSubmitting = false,
                    error = UiMessage.ErrorMessage.from(error = error)
                )
            }
            val exception = error as? DappRequestException
            if (exception != null) {
                if (!transactionRequest.isInternal) {
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        dappId = transactionRequest.dappId,
                        requestId = transactionRequest.requestId,
                        error = exception.failure.toWalletErrorType(),
                        message = exception.failure.getDappMessage()
                    )
                }
            }

            appEventBus.sendEvent(
                AppEvent.Status.Transaction.Fail(
                    requestId = transactionRequest.requestId,
                    transactionId = "",
                    isInternal = transactionRequest.isInternal,
                    errorMessage = UiMessage.ErrorMessage.from(exception?.failure)
                )
            )
        }
    }

    private fun TransactionManifest.attachGuarantees(previewType: PreviewType): TransactionManifest {
        var manifest = this
        if (previewType is PreviewType.Transaction) {
            previewType.to.map { it.resources }.flatten().forEach { depositing ->
                when (val assertion = depositing.guaranteeAssertion) {
                    is GuaranteeAssertion.ForAmount -> {
                        manifest = manifest.addGuaranteeInstructionToManifest(
                            address = depositing.transferable.resourceAddress,
                            guaranteedAmount = assertion.amount.toPlainString(),
                            index = assertion.instructionIndex.toInt()
                        )
                    }
                    is GuaranteeAssertion.ForNFT -> {
                        // TODO RET ask Matt if needs to be implemented
                    }
                    null -> {}
                }
            }
        }

        return manifest
    }
}
