package com.babylon.wallet.android.presentation.transaction.submit

import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.model.FeePayerSearchResult
import com.babylon.wallet.android.data.transaction.model.TransactionApprovalRequest
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.Event
import com.babylon.wallet.android.presentation.transaction.TransactionApprovalViewModel2.State
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.radixdlt.ret.TransactionManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.gateway.GetCurrentGatewayUseCase
import timber.log.Timber

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
        approvalJob = appScope.launch {
            val transactionRequest = state.value.request
            val currentNetworkId = getCurrentGatewayUseCase().network.networkId().value
            val manifestNetworkId = transactionRequest.transactionManifestData.networkId

            if (currentNetworkId != manifestNetworkId) {
                approvalJob = null
                val failure = DappRequestFailure.WrongNetwork(currentNetworkId, manifestNetworkId)
                onDismiss(failure = failure)
                return@launch
            }

            state.update { it.copy(isSubmitting = true) }

            var manifest = transactionRequest.transactionManifestData.toTransactionManifest()
            // TODO Adjust guarantees
//            manifest = manifest.addGuaranteeInstructionToManifest(
//                address = transactionAccountUiItem.resourceAddress.orEmpty(),
//                guaranteedAmount = guaranteedAmount,
//                index = transactionAccountUiItem.instructionIndex ?: 0
//            )

            transactionClient.findFeePayerInManifest(manifest)
                .onSuccess { feePayerResult ->
                    state.update { it.copy(isSubmitting = false) }
                    if (feePayerResult.feePayerExistsInManifest) {
                        signAndSubmit(
                            transactionRequest = transactionRequest,
                            feePayerSearchResult = feePayerResult,
                            manifest = manifest
                        )
                    } else {
                        // TODO Adjust fee payers
//                        state.update { state ->
//                            state.copy(
//                                feePayerCandidates = feePayerResult.candidates.map { it.toUiModel() }.toPersistentList(),
//                                bottomSheetViewMode = BottomSheetMode.FeePayerSelection
//                            )
//                        }
                        onSendScreenEvent(Event.SelectFeePayer)
                    }
                    approvalJob = null
                }
                .onFailure { error ->
                    state.update {
                        it.copy(isSubmitting = false, error = UiMessage.ErrorMessage.from(error = error))
                    }

                    if (error is DappRequestException && !transactionRequest.isInternal) {
                        dAppMessenger.sendWalletInteractionResponseFailure(
                            dappId = transactionRequest.dappId,
                            requestId = transactionRequest.requestId,
                            error = error.failure.toWalletErrorType(),
                            message = error.failure.getDappMessage()
                        )
                    }

                    approvalJob = null
                }
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
        } else {
            Timber.d("Cannot dismiss transaction while is in progress")
        }
    }

    @Suppress("LongMethod")
    private suspend fun signAndSubmit(
        transactionRequest: MessageFromDataChannel.IncomingRequest.TransactionRequest,
        feePayerSearchResult: FeePayerSearchResult,
        manifest: TransactionManifest
    ) {
        state.update { it.copy(isSigning = true) }
        val request = TransactionApprovalRequest(
            manifest = manifest,
            ephemeralNotaryPrivateKey = state.value.ephemeralNotaryPrivateKey,
            feePayerAddress = feePayerSearchResult.feePayerAddressFromManifest
        )
        transactionClient.signAndSubmitTransaction(request).onSuccess { txId ->
            state.update {
                it.copy(
                    isSigning = false,
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
                    isSigning = false,
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

        approvalJob = null
    }

}
