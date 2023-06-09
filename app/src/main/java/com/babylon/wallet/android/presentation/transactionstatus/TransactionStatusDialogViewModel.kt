package com.babylon.wallet.android.presentation.transactionstatus

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionStatusDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    val appEventBus: AppEventBus,
    private val savedStateHandle: SavedStateHandle
) : StateViewModel<TransactionStatusUiState>() {

    private val args = TransactionStatusDialogArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.TransactionEvent>().collect { event ->
                val status = TransactionStatus.from(event)
                _state.update { it.copy(transactionStatus = status) }
            }
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(state.value.transactionStatus.requestId)
        }
    }

    override fun initialState(): TransactionStatusUiState {
        return TransactionStatusUiState(transactionStatus = TransactionStatus.from(args.event))
    }
}

data class TransactionStatusUiState(
    val transactionStatus: TransactionStatus
) : UiState {

    val isCompleting: Boolean
        get() = transactionStatus is TransactionStatus.Completing

    val isSuccess: Boolean
        get() = transactionStatus is TransactionStatus.Success

    val isFailed: Boolean
        get() = transactionStatus is TransactionStatus.Failed

    val failureError: Int?
        get() = (transactionStatus as? TransactionStatus.Failed)?.messageRes

}

sealed interface TransactionStatus {

    val requestId: String
    val transactionId: String
    val isInternal: Boolean

    data class Completing(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean
    ) : TransactionStatus

    data class Success(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean
    ) : TransactionStatus

    data class Failed(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        @StringRes val messageRes: Int?
    ) : TransactionStatus

    companion object {
        fun from(event: AppEvent.TransactionEvent) = when (event) {
            is AppEvent.TransactionEvent.Failed -> Failed(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                messageRes = event.errorMessageRes
            )
            is AppEvent.TransactionEvent.Sent -> Completing(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
            )
            is AppEvent.TransactionEvent.Successful -> Success(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
            )
        }
    }
}
