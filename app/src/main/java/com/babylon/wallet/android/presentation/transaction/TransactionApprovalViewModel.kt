package com.babylon.wallet.android.presentation.transaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppMessenger
import com.babylon.wallet.android.data.dapp.model.WalletErrorType
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.model.IncomingRequest
import com.babylon.wallet.android.domain.model.TransactionManifestData
import com.babylon.wallet.android.domain.transaction.IncomingRequestHolder
import com.babylon.wallet.android.domain.transaction.TransactionApprovalException
import com.babylon.wallet.android.domain.transaction.TransactionApprovalFailure
import com.babylon.wallet.android.domain.transaction.TransactionClient
import com.babylon.wallet.android.presentation.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.ProfileRepository
import javax.inject.Inject

@HiltViewModel
class TransactionApprovalViewModel @Inject constructor(
    private val transactionClient: TransactionClient,
    private val incomingRequestHolder: IncomingRequestHolder,
    private val profileRepository: ProfileRepository,
    private val dAppMessenger: DAppMessenger,
) : ViewModel(), OneOffEventHandler<TransactionApprovalEvent> by OneOffEventHandlerImpl() {

    private lateinit var requestId: String

    internal var state by mutableStateOf(TransactionUiState())
        private set

    init {
        viewModelScope.launch {
            incomingRequestHolder.receive(viewModelScope) {
                if (it is IncomingRequest.TransactionWriteRequest) {
                    requestId = it.requestId
                    state = state.copy(manifestData = it.transactionManifestData, isLoading = false)
                }
            }
        }
    }

    fun approveTransaction() {
        viewModelScope.launch {
            state.manifestData?.let { manifestData ->
                val currentNetworkId = profileRepository.getCurrentNetworkId().value
                if (currentNetworkId != manifestData.networkId) {
                    val failure = TransactionApprovalFailure.WrongNetwork(currentNetworkId, manifestData.networkId)
                    val dappResult = dAppMessenger.sendTransactionWriteResponseFailure(
                        requestId,
                        failure.toWalletErrorType(),
                        failure.getMessage()
                    )
                    dappResult.onValue {
                        sendEvent(TransactionApprovalEvent.TransactionRejected)
                    }
                    dappResult.onError {
                        sendEvent(TransactionApprovalEvent.TransactionRejected)
                    }
                } else {
                    state = state.copy(isSigning = true)
                    val result = transactionClient.signAndSubmitTransaction(manifestData)
                    result.onValue { txId ->
                        state = state.copy(isSigning = false, approved = true)
                        dAppMessenger.sendTransactionWriteResponseSuccess(requestId, txId)
                    }
                    result.onError {
                        state = state.copy(isSigning = false, error = UiMessage(error = it))
                        val exception = it as? TransactionApprovalException
                        if (exception != null) {
                            dAppMessenger.sendTransactionWriteResponseFailure(
                                requestId,
                                error = exception.failure.toWalletErrorType(),
                                message = exception.failure.getMessage()
                            )
                        }
                    }
                }
            }
        }
    }

    fun onBackClick() {
        // TODO display dialog are we sure we want to reject transaction?
        viewModelScope.launch {
            val result =
                dAppMessenger.sendTransactionWriteResponseFailure(requestId, error = WalletErrorType.RejectedByUser)
            result.onValue {
                sendEvent(TransactionApprovalEvent.TransactionRejected)
            }
            result.onError {
                sendEvent(TransactionApprovalEvent.TransactionRejected)
            }
        }
    }

    fun onMessageShown() {
        state = state.copy(error = null)
    }
}

internal data class TransactionUiState(
    val manifestData: TransactionManifestData? = null,
    val isLoading: Boolean = true,
    val isSigning: Boolean = false,
    val approved: Boolean = false,
    val error: UiMessage? = null,
)

internal sealed interface TransactionApprovalEvent : OneOffEvent {
    object TransactionRejected : TransactionApprovalEvent
}
