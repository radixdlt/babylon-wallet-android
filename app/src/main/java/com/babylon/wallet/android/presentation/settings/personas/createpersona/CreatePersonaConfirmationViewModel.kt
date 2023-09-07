package com.babylon.wallet.android.presentation.settings.personas.createpersona

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class CreatePersonaConfirmationViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<CreatePersonaConfirmationViewModel.PersonaConfirmationUiState>(),
    OneOffEventHandler<CreatePersonaConfirmationEvent> by OneOffEventHandlerImpl() {

    override fun initialState(): PersonaConfirmationUiState = PersonaConfirmationUiState()

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
    ) : UiState
}

internal sealed interface CreatePersonaConfirmationEvent : OneOffEvent {
    object FinishPersonaCreation : CreatePersonaConfirmationEvent
}
