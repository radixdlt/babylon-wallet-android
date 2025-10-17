package com.babylon.wallet.android.presentation.timedrecovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.utils.AccessControllerTimedRecoveryStateObserver
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiMessage
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TimedRecoveryViewModel @Inject constructor(
    observer: AccessControllerTimedRecoveryStateObserver,
    savedStateHandle: SavedStateHandle
) : StateViewModel<TimedRecoveryViewModel.State>() {

    private val args = TimedRecoveryArgs(savedStateHandle)

    private var observeTimeJob: Job? = null

    init {
        observer.recoveryStateByAddress
            .map { states -> states[args.address] }
            .filterNotNull()
            .onEach { recoveryState ->
                val allowAfter = recoveryState.allowTimedRecoveryAfter?.unixTimestampSeconds?.toLongOrNull()
                    ?: return@onEach
                val remainingTime = allowAfter - Instant.now().epochSecond

                _state.update { state ->
                    state.copy(isLoading = false)
                }

                if (remainingTime > 0) {
                    observeTime(remainingTime.seconds)
                }
            }
            .launchIn(viewModelScope)
    }

    override fun initialState(): State = State(isLoading = true)

    fun onMessageShown() {
        _state.update { state -> state.copy(uiMessage = null) }
    }

    fun onConfirmClick() {
        // Prepare manifest and launch recovery reconfirmation transaction
    }

    fun onStopClick() {
        // Prepare manifest and launch stop recovery transaction
    }

    private fun observeTime(remainingTime: Duration) {
        var expirationDuration = remainingTime

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
            } while (remainingTime >= 0.seconds)
        }
    }

    data class State(
        val isLoading: Boolean,
        val uiMessage: UiMessage? = null,
        val remainingTime: Duration? = null
    ) : UiState {

        val canConfirm = remainingTime == null || remainingTime.inWholeSeconds <= 0
    }
}
