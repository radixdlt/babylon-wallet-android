package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.data.transaction.TransactionApprovalFailure
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.toPrettyString
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.NetworkRepository
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val networkRepository: NetworkRepository,
    deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DAppMessenger,
    @ApplicationScope private val appScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<TransactionApprovalEvent> by OneOffEventHandlerImpl() {

    private val args = TransactionApprovalArgs(savedStateHandle)

    internal var state by mutableStateOf(TransactionUiState(isDeviceSecure = deviceSecurityHelper.isDeviceSecure()))
        private set

    private var approvalJob: Job? = null

    init {
        viewModelScope.launch {
            val transactionWriteRequest = incomingRequestRepository.getTransactionWriteRequest(args.requestId)
            state = state.copy(
                manifestData = transactionWriteRequest.transactionManifestData
            )
            val manifestResult = transactionClient.addLockFeeToTransactionManifestData(
                transactionWriteRequest.transactionManifestData
            )
            manifestResult.onValue { manifestWithLockFee ->
                when (
                    val manifestInStringFormatConversionResult = transactionClient.manifestInStringFormat(
                        manifestWithLockFee
                    )
                ) {
                    is Result.Error -> {
                        state = state.copy(
                            isLoading = false,
                            error = UiMessage.ErrorMessage(manifestInStringFormatConversionResult.exception)
                        )
                    }
                    is Result.Success -> {
                        state = state.copy(
                            manifestString = manifestInStringFormatConversionResult.data.toPrettyString(),
                            manifestData = transactionWriteRequest.transactionManifestData,
                            canApprove = true,
                            isLoading = false
                        )
                    }
                }
            }
            manifestResult.onError { error ->
                state = state.copy(
                    isLoading = false,
                    error = UiMessage.ErrorMessage(error)
                )
                dAppMessenger.sendTransactionWriteResponseFailure(
                    args.requestId,
                    error = WalletErrorType.FailedToPrepareTransaction
                )
                incomingRequestRepository.requestHandled(args.requestId)
            }
        }
    }

    fun approveTransaction() {
        approvalJob = appScope.launch {
            state.manifestData?.let { manifestData ->
                val currentNetworkId = networkRepository.getCurrentNetworkId().value
                if (currentNetworkId != manifestData.networkId) {
                    val failure = TransactionApprovalFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
                    dAppMessenger.sendTransactionWriteResponseFailure(
                        args.requestId,
                        failure.toWalletErrorType(),
                        failure.getDappMessage()
                    )
                    sendEvent(TransactionApprovalEvent.NavigateBack)
                    incomingRequestRepository.requestHandled(args.requestId)
                    approvalJob = null
                } else {
                    state = state.copy(isSigning = true)
                    state.manifestData?.let { manifest ->
                        val result = transactionClient.signAndSubmitTransaction(manifest)
                        result.onValue { txId ->
                            state = state.copy(isSigning = false, approved = true)
                            dAppMessenger.sendTransactionWriteResponseSuccess(args.requestId, txId)
                            approvalJob = null
                            sendEvent(TransactionApprovalEvent.NavigateBack)
                            incomingRequestRepository.requestHandled(args.requestId)
                        }
                        result.onError {
                            state = state.copy(isSigning = false, error = UiMessage.ErrorMessage(error = it))
                            val exception = it as? TransactionApprovalException
                            if (exception != null) {
                                dAppMessenger.sendTransactionWriteResponseFailure(
                                    args.requestId,
                                    error = exception.failure.toWalletErrorType(),
                                    message = exception.failure.getDappMessage()
                                )
                                approvalJob = null
                                sendEvent(TransactionApprovalEvent.NavigateBack)
                                incomingRequestRepository.requestHandled(args.requestId)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onBackClick() {
        // TODO display dialog are we sure we want to reject transaction?
        viewModelScope.launch {
            if (approvalJob != null || state.approved) {
                sendEvent(TransactionApprovalEvent.NavigateBack)
            } else {
                dAppMessenger.sendTransactionWriteResponseFailure(
                    args.requestId,
                    error = WalletErrorType.RejectedByUser
                )
                sendEvent(TransactionApprovalEvent.NavigateBack)
                incomingRequestRepository.requestHandled(args.requestId)
            }
        }
    }

    fun onMessageShown() {
        state = state.copy(error = null)
    }
}

internal data class TransactionUiState(
    val manifestData: TransactionManifestData? = null,
    val manifestString: String = "",
    val isLoading: Boolean = true,
    val isSigning: Boolean = false,
    val approved: Boolean = false,
    val isDeviceSecure: Boolean = false,
    val error: UiMessage? = null,
    val canApprove: Boolean = false,
)

internal sealed interface TransactionApprovalEvent : OneOffEvent {
    object NavigateBack : TransactionApprovalEvent
}
