package com.babylon.wallet.android.presentation.accessfactorsources.createpersona

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesInput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesOutput
import com.babylon.wallet.android.presentation.accessfactorsources.AccessFactorSourcesUiProxy
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.ProfileException
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase
import javax.inject.Inject

@HiltViewModel
class CreatePersonaDialogViewModel @Inject constructor(
    private val createPersonaWithDeviceFactorSourceUseCase: CreatePersonaWithDeviceFactorSourceUseCase,
    private val accessFactorSourcesUiProxy: AccessFactorSourcesUiProxy
) : StateViewModel<CreatePersonaDialogViewModel.CreatePersonaDialogUiState>(),
    OneOffEventHandler<CreatePersonaDialogViewModel.Event> by OneOffEventHandlerImpl() {

    override fun initialState(): CreatePersonaDialogUiState = CreatePersonaDialogUiState()

    private lateinit var input: AccessFactorSourcesInput.CreatePersona
    private var createPersonaJob: Job? = null

    init {
        createPersonaJob = viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.CreatePersona
            sendEvent(Event.RequestBiometricPrompt)
        }
    }

    fun biometricAuthenticationCompleted() {
        viewModelScope.launch {
            input = accessFactorSourcesUiProxy.getInput() as AccessFactorSourcesInput.CreatePersona
            createPersonaWithDeviceFactorSourceUseCase(input.displayName, input.personaData).onSuccess { persona ->
                accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.CreatedPersona(persona))
                sendEvent(Event.AccessingFactorSourceCompleted)
            }.onFailure { e ->
                when (e) {
                    is ProfileException -> {
                        accessFactorSourcesUiProxy.setOutput(AccessFactorSourcesOutput.Failure(e))
                        sendEvent(Event.AccessingFactorSourceCompleted)
                    }

                    else -> {
                        _state.update { uiState ->
                            uiState.copy(shouldShowRetryButton = true)
                        }
                    }
                }
            }
        }
    }

    fun onBiometricAuthenticationDismiss() {
        // biometric prompt dismissed, but bottom dialog remains visible
        // therefore we show the retry button
        _state.update { uiState ->
            uiState.copy(shouldShowRetryButton = true)
        }
    }

    fun onRetryClick() {
        createPersonaJob?.cancel()
        createPersonaJob = viewModelScope.launch {
            _state.update { uiState ->
                uiState.copy(shouldShowRetryButton = false)
            }
            sendEvent(Event.RequestBiometricPrompt)
        }
    }

    fun onUserDismiss() {
        viewModelScope.launch {
            createPersonaJob?.cancel()
            accessFactorSourcesUiProxy.setOutput(
                output = AccessFactorSourcesOutput.Failure(CancellationException("User cancelled"))
            )
            sendEvent(Event.UserDismissed)
        }
    }

    data class CreatePersonaDialogUiState(
        val shouldShowRetryButton: Boolean = false
    ) : UiState

    sealed interface Event : OneOffEvent {
        data object RequestBiometricPrompt : Event
        data object AccessingFactorSourceCompleted : Event
        data object UserDismissed : Event
    }
}
