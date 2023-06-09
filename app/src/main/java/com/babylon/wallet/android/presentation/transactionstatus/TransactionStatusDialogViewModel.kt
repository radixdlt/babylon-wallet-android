package com.babylon.wallet.android.presentation.transactionstatus

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.common.onError
import com.babylon.wallet.android.domain.common.onValue
import com.babylon.wallet.android.domain.usecases.transaction.PollTransactionStatusUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.transfer.TransferViewModel
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import javax.inject.Inject

@HiltViewModel
class TransactionStatusDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val pollTransactionStatusUseCase: PollTransactionStatusUseCase,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransactionStatusDialogViewModel.State>(),
    OneOffEventHandler<TransactionStatusDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State {
        return State(status = TransactionStatus.from(args.event))
    }

    private val args = TransactionStatusDialogArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.TransactionEvent>()
                .filter {
                    it.requestId == state.value.status.requestId
                }.collect { event ->
                    val status = TransactionStatus.from(event)
                    _state.update { it.copy(status = status) }

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

    private fun pollTransactionStatus(status: TransactionStatus.Completing) = viewModelScope.launch {
        pollTransactionStatusUseCase(status.transactionId).onValue {
            // Notify the system and this particular dialog that the transaction is completed
            appEventBus.sendEvent(
                AppEvent.TransactionEvent.Successful(
                    requestId = status.requestId,
                    transactionId = status.transactionId,
                    isInternal = status.isInternal
                )
            )
        }.onError {
            // Notify the system and this particular dialog that an error has occurred
            appEventBus.sendEvent(
                AppEvent.TransactionEvent.Failed(
                    requestId = status.requestId,
                    transactionId = status.transactionId,
                    isInternal = status.isInternal,
                    errorMessageRes = UiMessage.ErrorMessage(it).getUserFriendlyDescriptionRes()
                )
            )
        }
    }

    fun onDismiss() {
        if (state.value.isCompleting) {
            _state.update { it.copy(isIgnoreTransactionModalShowing = true) }
        } else {
            onDismissConfirmed()
        }
    }

    fun onDismissConfirmed() {
        _state.update { it.copy(isIgnoreTransactionModalShowing = false) }
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(state.value.status.requestId)
            sendEvent(Event.DismissDialog)
        }
    }

    fun onDismissCanceled() {
        _state.update { it.copy(isIgnoreTransactionModalShowing = false) }
    }

    data class State(
        val status: TransactionStatus,
        val isIgnoreTransactionModalShowing: Boolean = false
    ) : UiState {

        val isCompleting: Boolean
            get() = status is TransactionStatus.Completing

        val isSuccess: Boolean
            get() = status is TransactionStatus.Success

        val isFailed: Boolean
            get() = status is TransactionStatus.Failed

        val failureError: Int?
            get() = (status as? TransactionStatus.Failed)?.messageRes

        val transactionId: String
            get() = status.transactionId

    }

    sealed interface Event : OneOffEvent {
        object DismissDialog : Event
    }
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


