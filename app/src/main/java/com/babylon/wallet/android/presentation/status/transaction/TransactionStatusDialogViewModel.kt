package com.babylon.wallet.android.presentation.status.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DappMessenger
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.data.transaction.DappRequestException
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionStatusDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val dAppMessenger: DappMessenger,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransactionStatusDialogViewModel.State>(),
    OneOffEventHandler<TransactionStatusDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State {
        return State(status = TransactionStatus.from(args.event), blockUntilComplete = args.event.blockUntilComplete)
    }

    private val args = TransactionStatusDialogArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.Status.Transaction>()
                .filter {
                    it.requestId == state.value.status.requestId
                }.collect { event ->
                    val status = TransactionStatus.from(event)
                    _state.update {
                        it.copy(
                            status = status,
                            isIgnoreTransactionModalShowing = if (status is TransactionStatus.Completing) {
                                it.isIgnoreTransactionModalShowing
                            } else {
                                false
                            }
                        )
                    }

                    if (status is TransactionStatus.Completing) {
                        pollTransactionStatus(status)
                    }
                }
        }

        val initialStatus = state.value.status
        if (initialStatus is TransactionStatus.Completing) {
            pollTransactionStatus(initialStatus)
        }
    }

    private fun pollTransactionStatus(status: TransactionStatus.Completing) {
        viewModelScope.launch {
            transactionStatusClient.listenForPollStatus(status.transactionId).collect { pollResult ->
                pollResult.result.onSuccess {
                    // Notify the system and this particular dialog that the transaction is completed
                    appEventBus.sendEvent(
                        AppEvent.Status.Transaction.Success(
                            requestId = status.requestId,
                            transactionId = status.transactionId,
                            isInternal = status.isInternal,
                            blockUntilComplete = status.blockUntilComplete
                        )
                    )
                }.onFailure { error ->
                    if (!status.isInternal) {
                        (error as? DappRequestException)?.let { exception ->
                            val request = incomingRequestRepository.getTransactionWriteRequest(status.requestId)
                            dAppMessenger.sendWalletInteractionResponseFailure(
                                remoteConnectorId = request.remoteConnectorId,
                                requestId = status.requestId,
                                error = exception.failure.toWalletErrorType(),
                                message = exception.failure.getDappMessage()
                            )
                        }
                    }

                    // Notify the system and this particular dialog that an error has occurred
                    appEventBus.sendEvent(
                        AppEvent.Status.Transaction.Fail(
                            requestId = status.requestId,
                            transactionId = status.transactionId,
                            isInternal = status.isInternal,
                            errorMessage = UiMessage.ErrorMessage.from(error),
                            blockUntilComplete = status.blockUntilComplete
                        )
                    )
                }
                transactionStatusClient.statusHandled(status.transactionId)
            }
        }
    }

    fun onDismiss() {
        if (state.value.isCompleting && args.event.blockUntilComplete) return
        if (state.value.isCompleting) {
            _state.update { it.copy(isIgnoreTransactionModalShowing = true) }
        } else {
            onDismissConfirmed()
        }
    }

    fun onDismissConfirmed() {
        _state.update { it.copy(isIgnoreTransactionModalShowing = false) }
        viewModelScope.launch {
            sendEvent(Event.DismissDialog)
            incomingRequestRepository.requestHandled(state.value.status.requestId)
        }
    }

    fun onDismissCanceled() {
        _state.update { it.copy(isIgnoreTransactionModalShowing = false) }
    }

    data class State(
        val status: TransactionStatus,
        val blockUntilComplete: Boolean,
        val isIgnoreTransactionModalShowing: Boolean = false
    ) : UiState {

        val isCompleting: Boolean
            get() = status is TransactionStatus.Completing

        val isSuccess: Boolean
            get() = status is TransactionStatus.Success

        val isFailed: Boolean
            get() = status is TransactionStatus.Failed

        val failureError: UiMessage.ErrorMessage?
            get() = (status as? TransactionStatus.Failed)?.errorMessage
    }

    sealed interface Event : OneOffEvent {
        object DismissDialog : Event
    }
}

sealed interface TransactionStatus {

    val requestId: String
    val transactionId: String
    val isInternal: Boolean
    val blockUntilComplete: Boolean

    data class Completing(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean
    ) : TransactionStatus

    data class Success(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean
    ) : TransactionStatus

    data class Failed(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean,
        val errorMessage: UiMessage.ErrorMessage?
    ) : TransactionStatus

    companion object {
        fun from(event: AppEvent.Status.Transaction) = when (event) {
            is AppEvent.Status.Transaction.Fail -> Failed(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                errorMessage = event.errorMessage,
                blockUntilComplete = event.blockUntilComplete
            )

            is AppEvent.Status.Transaction.InProgress -> Completing(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                blockUntilComplete = event.blockUntilComplete
            )

            is AppEvent.Status.Transaction.Success -> Success(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                blockUntilComplete = event.blockUntilComplete
            )
        }
    }
}
