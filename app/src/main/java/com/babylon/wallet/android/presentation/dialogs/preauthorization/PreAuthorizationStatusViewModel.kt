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
import com.radixdlt.sargon.AddressFormat
import com.radixdlt.sargon.TransactionIntentHash
import com.radixdlt.sargon.extensions.formatted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
                        processExpiration(event.remainingTime)
                    }
                }
        }

        if (args.event is AppEvent.Status.PreAuthorization.Sent) {
            pollTransactionStatus(args.event)
            processExpiration(args.event.remainingTime)
        }
    }

    override fun initialState(): State {
        return State(status = State.Status.from(args.event))
    }

    private fun pollTransactionStatus(status: AppEvent.Status.PreAuthorization) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            transactionStatusClient.listenForPreAuthorizationPollStatus(args.event.encodedPreAuthorizationId).collectLatest { pollResult ->
                when (pollResult.result) {
                    is PreAuthorizationStatusData.Status.Success -> {
                        // Notify the system and this particular dialog that the transaction is completed
                        appEventBus.sendEvent(
                            AppEvent.Status.PreAuthorization.Success(
                                requestId = args.event.requestId,
                                preAuthorizationId = args.event.preAuthorizationId,
                                isMobileConnect = args.event.isMobileConnect,
                                dAppName = status.dAppName,
                                transactionId = pollResult.result.txIntentHash
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

                transactionStatusClient.statusHandled(args.event.encodedPreAuthorizationId)
            }
        }
    }

    private fun processExpiration(remainingTime: Duration) {
        var expirationDuration = remainingTime

        viewModelScope.launch {
            do {
                _state.update {
                    it.copy(
                        status = (it.status as? State.Status.Sent)?.copy(
                            expiration = State.Status.Sent.Expiration(duration = expirationDuration)
                        ) ?: it.status
                    )
                }
                expirationDuration -= 1.seconds
                delay(1.seconds)
            } while (remainingTime >= 0.seconds && state.value.status is State.Status.Sent)
        }
    }

    fun onCopyPreAuthorizationIdClick() {
        viewModelScope.launch {
            sendEvent(Event.PerformCopy(valueToCopy = args.event.encodedPreAuthorizationId))
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

            data class Sent(
                val preAuthorizationId: String,
                val dAppName: String?,
                val expiration: Expiration
            ) : Status {

                data class Expiration(
                    val duration: Duration
                ) {

                    val isCheckingOneLastTime: Boolean
                        get() = duration < 1.seconds

                    val truncateSeconds: Boolean
                        get() = duration >= 60.seconds
                }
            }

            data class Success(
                val transactionId: TransactionIntentHash,
                val isMobileConnect: Boolean
            ) : Status

            data class Expired(
                val isMobileConnect: Boolean
            ) : Status

            companion object {

                fun from(event: AppEvent.Status.PreAuthorization) = when (event) {
                    is AppEvent.Status.PreAuthorization.Expired -> Expired(
                        isMobileConnect = event.isMobileConnect
                    )
                    is AppEvent.Status.PreAuthorization.Sent -> Sent(
                        preAuthorizationId = event.preAuthorizationId.formatted(AddressFormat.DEFAULT),
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

        data class PerformCopy(val valueToCopy: String) : Event

        data object Dismiss : Event
    }
}
