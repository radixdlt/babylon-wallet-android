package com.babylon.wallet.android.presentation.dialogs.preauthorization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.repository.PreAuthorizationStatusData
import com.babylon.wallet.android.data.repository.TransactionStatusClient
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.AppEvent
import com.babylon.wallet.android.utils.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class PreAuthorizationStatusViewModel @Inject constructor(
    private val incomingRequestRepository: IncomingRequestRepository,
    private val transactionStatusClient: TransactionStatusClient,
    private val appEventBus: AppEventBus,
    savedStateHandle: SavedStateHandle
) : StateViewModel<PreAuthorizationStatusViewModel.State>(),
    OneOffEventHandler<PreAuthorizationStatusViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = PreAuthorizationStatusDialogArgs(savedStateHandle)
    private var pollJob: Job? = null
    private var isRequestHandled = false

    init {
        viewModelScope.launch {
            appEventBus.events
                .filterIsInstance<AppEvent.Status.PreAuthorization>()
                .filter {
                    it.requestId == args.event.requestId
                }.collect { event ->
                    val status = State.Status.from(event)
                    _state.update { it.copy(status = status) }

                    if (event is AppEvent.Status.PreAuthorization.Sent) {
                        pollTransactionStatus(event)
                    }
                }
        }

        if (args.event is AppEvent.Status.PreAuthorization.Sent) {
            pollTransactionStatus(args.event)
        }
    }

    override fun initialState(): State {
        return State(status = State.Status.from(args.event))
    }

    private fun pollTransactionStatus(status: AppEvent.Status.PreAuthorization) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            transactionStatusClient.listenForPreAuthorizationPollStatus(args.event.preAuthorizationId).collectLatest { pollResult ->
                when (pollResult.result) {
                    is PreAuthorizationStatusData.Status.Success -> {
                        // Notify the system and this particular dialog that the transaction is completed
                        appEventBus.sendEvent(
                            AppEvent.Status.PreAuthorization.Success(
                                requestId = args.event.requestId,
                                preAuthorizationId = args.event.preAuthorizationId,
                                isMobileConnect = args.event.isMobileConnect,
                                dAppName = status.dAppName,
                                transactionId = pollResult.result.txIntentHash.bech32EncodedTxId
                            )
                        )
                        markRequestAsHandled()
                    }
                    PreAuthorizationStatusData.Status.Expired -> {
                        // Notify the system and this particular dialog that the pre-authorization has expired
                        appEventBus.sendEvent(
                            AppEvent.Status.PreAuthorization.Expired(
                                requestId = status.requestId,
                                preAuthorizationId = args.event.preAuthorizationId,
                                isMobileConnect = status.isMobileConnect,
                                dAppName = status.dAppName
                            )
                        )
                    }
                }

                transactionStatusClient.statusHandled(args.event.preAuthorizationId)
            }
        }
    }

    fun onDismiss() {
        viewModelScope.launch {
            markRequestAsHandled()
            sendEvent(Event.Dismiss)
        }
    }

    private suspend fun markRequestAsHandled() {
        if (isRequestHandled) {
            return
        }

        incomingRequestRepository.requestHandled(args.event.requestId)
        isRequestHandled = true
    }

    data class State(
        val status: Status
    ) : UiState {

        sealed interface Status {

            data class Expired(
                val isMobileConnect: Boolean
            ) : Status

            data class Success(
                val transactionId: String,
                val isMobileConnect: Boolean
            ) : Status

            data class Sent(
                val preAuthorizationId: String,
                val dAppName: String?,
                val expiration: Expiration
            ) : Status {

                data class Expiration(
                    val duration: Duration
                ) {

                    val isExpired: Boolean
                        get() = duration == 0.seconds

                    val truncateSeconds: Boolean
                        get() = duration >= 60.seconds
                }
            }

            companion object {

                fun from(event: AppEvent.Status.PreAuthorization) = when (event) {
                    is AppEvent.Status.PreAuthorization.Expired -> Expired(
                        isMobileConnect = event.isMobileConnect
                    )
                    is AppEvent.Status.PreAuthorization.Sent -> Sent(
                        preAuthorizationId = event.preAuthorizationId,
                        dAppName = event.dAppName,
                        expiration = Sent.Expiration(
                            duration = event.remainingTime
                        )
                    )
                    is AppEvent.Status.PreAuthorization.Success -> Success(
                        transactionId = event.transactionId,
                        isMobileConnect = event.isMobileConnect
                    )
                }
            }
        }
    }

    sealed interface Event : OneOffEvent {
        data object Dismiss : Event
    }
}
