package com.babylon.wallet.android.presentation.accessfactorsources.authorization

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.usecases.BiometricsAuthenticateUseCase
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesIOHandler
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.AuthorizationPurpose
import com.radixdlt.sargon.AuthorizationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestAuthorizationViewModel @Inject constructor(
    private val accessFactorSourcesIOHandler: AccessFactorSourcesIOHandler,
    private val biometricsAuthenticateUseCase: BiometricsAuthenticateUseCase
) : StateViewModel<RequestAuthorizationViewModel.State>(),
    OneOffEventHandler<RequestAuthorizationViewModel.Event> by OneOffEventHandlerImpl() {

    private val proxyInput = accessFactorSourcesIOHandler.getInput() as AccessFactorSourcesInput.ToRequestAuthorization
    private var authorizationJob: Job? = null

    override fun initialState(): State = State(
        purpose = proxyInput.purpose,
        isRequestingAuthorization = true
    )

    init {
        authorizationJob = viewModelScope.launch { authorize() }
    }

    fun onDismiss() {
        authorizationJob?.cancel()

        viewModelScope.launch {
            sendEvent(Event.Completed)
            accessFactorSourcesIOHandler.setOutput(AccessFactorSourcesOutput.RequestAuthorization(output = AuthorizationResponse.REJECTED))
        }
    }

    fun onRetry() {
        authorizationJob?.cancel()

        authorizationJob = viewModelScope.launch { authorize() }
    }

    private suspend fun authorize() {
        _state.update { it.copy(isRequestingAuthorization = true) }

        if (biometricsAuthenticateUseCase()) {
            sendEvent(Event.Completed)
            accessFactorSourcesIOHandler.setOutput(
                AccessFactorSourcesOutput.RequestAuthorization(output = AuthorizationResponse.AUTHORIZED)
            )
        }

        _state.update { it.copy(isRequestingAuthorization = false) }
    }

    data class State(
        val purpose: AuthorizationPurpose,
        val isRequestingAuthorization: Boolean,
    ) : UiState {

        val isRetryEnabled: Boolean
            get() = !isRequestingAuthorization
    }

    sealed interface Event : OneOffEvent {
        data object Completed : Event
    }
}
