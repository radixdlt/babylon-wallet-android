package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class CreatePersonaConfirmationViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel(), OneOffEventHandler<CreatePersonaConfirmationEvent> by OneOffEventHandlerImpl() {

    init {
        viewModelScope.launch {
            personaUiState = personaUiState.copy(
                isFirstPersona = getProfileUseCase.personasOnCurrentNetwork().count() == 1
            )
        }
    }

    var personaUiState by mutableStateOf(PersonaConfirmationUiState())
        private set

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
