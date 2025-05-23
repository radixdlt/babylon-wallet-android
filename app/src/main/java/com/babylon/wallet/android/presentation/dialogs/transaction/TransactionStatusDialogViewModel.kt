package com.babylon.wallet.android.presentation.dialogs.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.domain.RadixWalletException
import com.babylon.wallet.android.domain.asRadixWalletException
import com.babylon.wallet.android.domain.toDappWalletInteractionErrorType
import com.babylon.wallet.android.domain.usecases.RespondToIncomingRequestUseCase
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import com.babylon.wallet.android.utils.ExceptionMessageProvider
import com.radixdlt.sargon.DappWalletInteractionErrorType
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.extensions.init
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class TransactionStatusDialogViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val respondToIncomingRequestUseCase: RespondToIncomingRequestUseCase,
    private val appEventBus: AppEventBus,
    private val exceptionMessageProvider: ExceptionMessageProvider,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TransactionStatusDialogViewModel.State>(),
    OneOffEventHandler<TransactionStatusDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State {
        return State(
            status = TransactionStatus.from(args.event),
            blockUntilComplete = args.event.blockUntilComplete
        )
    }

    private val args = TransactionStatusDialogArgs(savedStateHandle)
    private var observeStatusJob: Job? = null
    private var isRequestHandled = false

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
                            dismissInfo = it.dismissInfo.takeIf { status is TransactionStatus.Completing }
                        )
                    }

                    if (status is TransactionStatus.Completing) {
                        observeTransactionStatus(status)
                    }
                }
        }

        val initialStatus = state.value.status
        if (initialStatus is TransactionStatus.Completing) {
            observeTransactionStatus(initialStatus)
        }
    }

    private fun observeTransactionStatus(status: TransactionStatus.Completing) {
        observeStatusJob?.cancel()
        observeStatusJob = viewModelScope.launch {
            transactionStatusClient.listenForTransactionStatus(status.transactionId).collectLatest { result ->
                result.result.onSuccess {
                    // Notify the system and this particular dialog that the transaction is completed
                    markRequestAsHandled()
                    appEventBus.sendEvent(
                        AppEvent.Status.Transaction.Success(
                            requestId = status.requestId,
                            transactionId = status.transactionId,
                            isInternal = status.isInternal,
                            blockUntilComplete = status.blockUntilComplete,
                            isMobileConnect = status.isMobileConnect,
                            dAppName = status.dAppName
                        )
                    )
                }.onFailure { error ->
                    markRequestAsHandled()
                    if (!status.isInternal) {
                        (error as? RadixWalletException.TransactionSubmitException)?.let { exception ->
                            incomingRequestRepository.getRequest(status.requestId)?.let { transactionRequest ->
                                respondToIncomingRequestUseCase.respondWithFailure(
                                    request = transactionRequest,
                                    dappWalletInteractionErrorType = exception.dappWalletInteractionErrorType,
                                    message = exception.getDappMessage()
                                )
                            }
                        }
                    }

                    // Notify the system and this particular dialog that an error has occurred
                    appEventBus.sendEvent(
                        AppEvent.Status.Transaction.Fail(
                            requestId = status.requestId,
                            transactionId = status.transactionId,
                            isInternal = status.isInternal,
                            errorMessage = exceptionMessageProvider.throwableMessage(error),
                            blockUntilComplete = status.blockUntilComplete,
                            walletErrorType = error.asRadixWalletException()?.toDappWalletInteractionErrorType(),
                            isMobileConnect = status.isMobileConnect,
                            dAppName = status.dAppName
                        )
                    )
                }
                transactionStatusClient.statusHandled(status.transactionId)
            }
        }
    }

    fun onDismiss() {
        when {
            state.value.isCompleting && args.event.blockUntilComplete -> {
                _state.update { it.copy(dismissInfo = State.DismissInfo.REQUIRE_COMPLETION) }
            }
            state.value.isCompleting -> {
                _state.update { it.copy(dismissInfo = State.DismissInfo.STOP_WAITING) }
            }
            else -> onDismissConfirmed()
        }
    }

    fun onInfoClose(confirmed: Boolean) {
        if (state.value.dismissInfo == State.DismissInfo.STOP_WAITING && confirmed) {
            onDismissConfirmed()
        }
        _state.update { it.copy(dismissInfo = null) }
    }

    private fun onDismissConfirmed() {
        viewModelScope.launch {
            markRequestAsHandled()
            sendEvent(Event.DismissDialog)
        }
    }

    private suspend fun markRequestAsHandled() {
        if (isRequestHandled) {
            return
        }

        incomingRequestRepository.requestHandled(state.value.status.requestId)
        isRequestHandled = true
    }

    data class State(
        val status: TransactionStatus,
        private val blockUntilComplete: Boolean,
        val dismissInfo: DismissInfo? = null
    ) : UiState {

        val statusEnum: StatusEnum
            get() = when (status) {
                is TransactionStatus.Completing -> StatusEnum.COMPLETING
                is TransactionStatus.Failed -> StatusEnum.FAIL
                is TransactionStatus.Success -> StatusEnum.SUCCESS
            }

        val isDismissible: Boolean
            get() = when (status) {
                is TransactionStatus.Completing -> !blockUntilComplete
                is TransactionStatus.Failed -> true
                is TransactionStatus.Success -> true
            }

        val isCompleting: Boolean
            get() = status is TransactionStatus.Completing

        val isSuccess: Boolean
            get() = status is TransactionStatus.Success

        val isFailed: Boolean
            get() = status is TransactionStatus.Failed

        val failureError: String?
            get() = (status as? TransactionStatus.Failed)?.errorMessage

        val walletErrorType: DappWalletInteractionErrorType?
            get() = (status as? TransactionStatus.Failed)?.walletErrorType

        val transactionId: TransactionIntentHash?
            get() = runCatching { TransactionIntentHash.init(status.transactionId) }.getOrNull()

        enum class DismissInfo {
            STOP_WAITING,
            REQUIRE_COMPLETION
        }

        enum class StatusEnum {
            COMPLETING,
            SUCCESS,
            FAIL
        }
    }

    sealed interface Event : OneOffEvent {
        data object DismissDialog : Event
    }
}

sealed interface TransactionStatus {

    val requestId: String
    val transactionId: String
    val isInternal: Boolean
    val blockUntilComplete: Boolean
    val isMobileConnect: Boolean
    val dAppName: String?

    data class Completing(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean,
        override val isMobileConnect: Boolean,
        override val dAppName: String?
    ) : TransactionStatus

    data class Success(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean,
        override val isMobileConnect: Boolean,
        override val dAppName: String?
    ) : TransactionStatus

    data class Failed(
        override val requestId: String,
        override val transactionId: String,
        override val isInternal: Boolean,
        override val blockUntilComplete: Boolean,
        val errorMessage: String?,
        val walletErrorType: DappWalletInteractionErrorType?,
        override val isMobileConnect: Boolean,
        override val dAppName: String?
    ) : TransactionStatus

    companion object {
        fun from(event: AppEvent.Status.Transaction) = when (event) {
            is AppEvent.Status.Transaction.Fail -> Failed(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                errorMessage = event.errorMessage,
                blockUntilComplete = event.blockUntilComplete,
                walletErrorType = event.walletErrorType,
                isMobileConnect = event.isMobileConnect,
                dAppName = event.dAppName
            )

            is AppEvent.Status.Transaction.InProgress -> Completing(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                blockUntilComplete = event.blockUntilComplete,
                isMobileConnect = event.isMobileConnect,
                dAppName = event.dAppName
            )

            is AppEvent.Status.Transaction.Success -> Success(
                requestId = event.requestId,
                transactionId = event.transactionId,
                isInternal = event.isInternal,
                blockUntilComplete = event.blockUntilComplete,
                isMobileConnect = event.isMobileConnect,
                dAppName = event.dAppName
            )
        }
    }
}
