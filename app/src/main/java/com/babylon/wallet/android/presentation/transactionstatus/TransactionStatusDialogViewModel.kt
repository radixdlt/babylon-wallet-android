package com.babylon.wallet.android.presentation.transactionstatus

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
    private val appEventBus: AppEventBus,
    private val savedStateHandle: SavedStateHandle
) : StateViewModel<TransactionStatusUiState>() {

    private val args = TransactionStatusDialogArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            appEventBus.events.filterIsInstance<AppEvent.TransactionEvent>().collect { event ->
                when (event) {
                    is AppEvent.TransactionEvent.Failed -> {
                        _state.update { it.copy(transactionStatus = TransactionStatus.Failed(event.errorTextRes)) }
                    }
                    is AppEvent.TransactionEvent.Successful -> {
                        _state.update { it.copy(transactionStatus = TransactionStatus.Success) }
                    }
                    is AppEvent.TransactionEvent.Sent -> {
                        _state.update { it.copy(transactionStatus = TransactionStatus.Completing) }
                    }
                }
            }
        }
    }

    fun incomingRequestHandled() {
        viewModelScope.launch {
            incomingRequestRepository.requestHandled(args.requestId)
        }
    }

    override fun initialState(): TransactionStatusUiState {
        return TransactionStatusUiState()
    }
}

data class TransactionStatusUiState(
    val transactionStatus: TransactionStatus = TransactionStatus.Completing
) : UiState

sealed interface TransactionStatus {
    object Completing : TransactionStatus
    object Success : TransactionStatus
    data class Failed(val errorTextRes: Int?) : TransactionStatus
}
