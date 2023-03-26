package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.data.transaction.DappRequestFailure
import com.babylon.wallet.android.data.transaction.TransactionApprovalException
import com.babylon.wallet.android.data.transaction.TransactionClient
import com.babylon.wallet.android.data.transaction.toPrettyString
import com.babylon.wallet.android.di.coroutines.ApplicationScope
import com.babylon.wallet.android.domain.common.Result
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileDataSource
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val profileDataSource: ProfileDataSource,
    deviceSecurityHelper: DeviceSecurityHelper,
    private val dAppMessenger: DappMessenger,
    @ApplicationScope private val appScope: CoroutineScope,
    private val appEventBus: AppEventBus,
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
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = args.dappId,
                    requestId = args.requestId,
                    error = WalletErrorType.FailedToFindAccountWithEnoughFundsToLockFee
                )
            }
        }
    }

    @Suppress("LongMethod")
    fun approveTransaction() {
        approvalJob = appScope.launch {
            state.manifestData?.let { manifestData ->
                val currentNetworkId = profileDataSource.getCurrentNetworkId().value
                if (currentNetworkId != manifestData.networkId) {
                    val failure = DappRequestFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
                    dAppMessenger.sendWalletInteractionResponseFailure(
                        dappId = args.dappId,
                        requestId = args.requestId,
                        error = failure.toWalletErrorType(),
                        message = failure.getDappMessage()
                    )
                    sendEvent(TransactionApprovalEvent.NavigateBack)
                    approvalJob = null
                } else {
                    state = state.copy(isSigning = true)
                    state.manifestData?.let { manifest ->
                        val result = transactionClient.signAndSubmitTransaction(manifest)
                        result.onValue { txId ->
                            // Send confirmation to the dApp that tx was submitted before status polling
                            dAppMessenger.sendTransactionWriteResponseSuccess(
                                dappId = args.dappId,
                                requestId = args.requestId,
                                txId = txId
                            )

                            val transactionStatus = transactionClient.pollTransactionStatus(txId)
                            transactionStatus.onValue {
                                state = state.copy(isSigning = false, approved = true)
                                approvalJob = null
                                appEventBus.sendEvent(AppEvent.ApprovedTransaction)
                                sendEvent(TransactionApprovalEvent.NavigateBack)
                            }
                            transactionStatus.onError {
                                state = state.copy(isSigning = false, error = UiMessage.ErrorMessage(error = it))
                                val exception = it as? TransactionApprovalException
                                if (exception != null) {
                                    dAppMessenger.sendWalletInteractionResponseFailure(
                                        dappId = args.dappId,
                                        requestId = args.requestId,
                                        error = exception.failure.toWalletErrorType(),
                                        message = exception.failure.getDappMessage()
                                    )
                                    approvalJob = null
                                    sendEvent(TransactionApprovalEvent.NavigateBack)
                                }
                            }
                        }
                        result.onError {
                            state = state.copy(isSigning = false, error = UiMessage.ErrorMessage(error = it))
                            val exception = it as? TransactionApprovalException
                            if (exception != null) {
                                dAppMessenger.sendWalletInteractionResponseFailure(
                                    dappId = args.dappId,
                                    requestId = args.requestId,
                                    error = exception.failure.toWalletErrorType(),
                                    message = exception.failure.getDappMessage()
                                )
                                approvalJob = null
                                sendEvent(TransactionApprovalEvent.NavigateBack)
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
                dAppMessenger.sendWalletInteractionResponseFailure(
                    dappId = args.dappId,
                    requestId = args.requestId,
                    error = WalletErrorType.RejectedByUser
                )
                sendEvent(TransactionApprovalEvent.NavigateBack)
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
