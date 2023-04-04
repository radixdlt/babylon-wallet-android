package com.babylon.wallet.android.presentation.createpersona

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class CreatePersonaConfirmationViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel(), OneOffEventHandler<CreatePersonaConfirmationEvent> by OneOffEventHandlerImpl() {

    private val _state = MutableStateFlow(PersonaConfirmationUiState())
    val state: StateFlow<PersonaConfirmationUiState> = _state

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(isFirstPersona = getProfileUseCase.personasOnCurrentNetwork().count() == 1)
            }
        }
    }

    fun personaConfirmed() {
        viewModelScope.launch {
            sendEvent(CreatePersonaConfirmationEvent.FinishPersonaCreation)
        }
    }

    data class PersonaConfirmationUiState(
        val isFirstPersona: Boolean = true
    )
}

internal sealed interface CreatePersonaConfirmationEvent : OneOffEvent {
    object FinishPersonaCreation : CreatePersonaConfirmationEvent
}
