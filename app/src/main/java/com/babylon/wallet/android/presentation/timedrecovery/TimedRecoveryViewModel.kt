package com.babylon.wallet.android.presentation.timedrecovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.data.dapp.model.TransactionType
import com.babylon.wallet.android.di.coroutines.DefaultDispatcher
import com.babylon.wallet.android.domain.model.transaction.UnvalidatedManifestData
import com.babylon.wallet.android.domain.model.transaction.prepareInternalTransactionRequest
import com.babylon.wallet.android.domain.utils.AccessControllerStateDetailsObserver
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.FULL_DATE_FORMAT
import com.babylon.wallet.android.utils.callSafely
import com.babylon.wallet.android.utils.toDateString
import com.radixdlt.sargon.SecurityStructureOfFactorSources
import com.radixdlt.sargon.TransactionManifest
import com.radixdlt.sargon.extensions.blobs
import com.radixdlt.sargon.extensions.bytes
import com.radixdlt.sargon.extensions.manifestString
import com.radixdlt.sargon.extensions.toList
import com.radixdlt.sargon.os.SargonOsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.map
import kotlin.onSuccess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TimedRecoveryViewModel @Inject constructor(
    private val sargonOsManager: SargonOsManager,
    private val incomingRequestRepository: IncomingRequestRepository,
    private val observer: AccessControllerStateDetailsObserver,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TimedRecoveryViewModel.State>(),
    OneOffEventHandler<TimedRecoveryViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = TimedRecoveryArgs(savedStateHandle)

    private var observeTimeJob: Job? = null

    init {
        initConfig()
        observeAcState()
    }

    override fun initialState(): State = State(isLoading = true)

    fun onMessageShown() {
        _state.update { state -> state.copy(uiMessage = null) }
    }

    fun onConfirmClick() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                makeConfirmTimedRecoveryManifest(args.address)
            }.mapCatching { manifest ->
                addTransaction(
                    manifest = manifest,
                    type = TransactionType.ConfirmSecurityStructureRecovery(
                        entityAddress = args.address
                    )
                )
            }
        }
    }

    fun onStopClick() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                makeStopTimedRecoveryManifest(args.address)
            }.mapCatching { manifest ->
                addTransaction(
                    manifest = manifest,
                    type = TransactionType.StopSecurityStructureRecovery(
                        entityAddress = args.address
                    )
                )
            }
        }
    }

    private fun initConfig() {
        viewModelScope.launch {
            sargonOsManager.callSafely(dispatcher) {
                val isProposalUnknown = isRecoveryProposalUnknown(
                    entityAddress = args.address
                )
                val structure = if (!isProposalUnknown) {
                    provisionalSecurityStructureOfFactorSourcesFromAddressOfAccountOrPersona(
                        addressOfAccountOrPersona = args.address
                    )
                } else {
                    null
                }
                isProposalUnknown to structure
            }.onFailure {
                _state.update { state ->
                    state.copy(
                        uiMessage = UiMessage.ErrorMessage(it)
                    )
                }
            }.onSuccess {
                _state.update { state ->
                    state.copy(
                        isRecoveryProposalUnknown = it.first,
                        securityStructure = it.second
                    )
                }
            }
        }
    }

    private fun observeAcState() {
        observer.acStateByEntityAddress
            .map { states -> states[args.address] }
            .filterNotNull()
            .onEach { recoveryState ->
                val allowAfter =
                    recoveryState.timedRecoveryState?.allowTimedRecoveryAfterUnixTimestampSeconds?.toLongOrNull()
                        ?: return@onEach
                val remainingTime = allowAfter - Instant.now().epochSecond

                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        confirmationDate = Instant.ofEpochSecond(allowAfter).toDateString(
                            format = FULL_DATE_FORMAT
                        )
                    )
                }

                if (remainingTime > 0) {
                    observeTime(remainingTime.seconds)
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun addTransaction(manifest: TransactionManifest, type: TransactionType) {
        runCatching {
            UnvalidatedManifestData(
                instructions = manifest.manifestString,
                plainMessage = null,
                networkId = sargonOsManager.sargonOs.currentNetworkId(),
                blobs = manifest.blobs.toList().map { it.bytes },
            ).prepareInternalTransactionRequest(
                transactionType = type,
                blockUntilCompleted = true
            )
        }.onSuccess {
            sendEvent(Event.Dismiss)
            incomingRequestRepository.add(it)
        }.onFailure { error ->
            _state.update { state -> state.copy(uiMessage = UiMessage.ErrorMessage(error)) }
        }
    }

    private fun observeTime(remainingTime: Duration) {
        var expirationDuration = remainingTime + 1.seconds

        observeTimeJob?.cancel()
        observeTimeJob = viewModelScope.launch {
            do {
                _state.update { state ->
                    state.copy(
                        remainingTime = expirationDuration
                    )
                }
                expirationDuration -= 1.seconds
                delay(1.seconds)
            } while (expirationDuration >= 0.seconds)
        }
    }

    data class State(
        val isLoading: Boolean,
        val uiMessage: UiMessage? = null,
        val remainingTime: Duration? = null,
        val confirmationDate: String? = null,
        val securityStructure: SecurityStructureOfFactorSources? = null,
        val isRecoveryProposalUnknown: Boolean = false
    ) : UiState {

        val canConfirm = (remainingTime == null || remainingTime.inWholeSeconds <= 0) && !isRecoveryProposalUnknown
    }

    sealed interface Event : OneOffEvent {

        data object Dismiss : Event
    }
}
