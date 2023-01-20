package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.truncatedHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.repository.PersonaRepository
import javax.inject.Inject

@HiltViewModel
class CreatePersonaConfirmationViewModel @Inject constructor(
    private val personaRepository: PersonaRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OneOffEventHandler<CreatePersonaConfirmationEvent> by OneOffEventHandlerImpl() {

    private val args = CreatePersonaConfirmationArgs(savedStateHandle)

    init {
        viewModelScope.launch {
            val persona = personaRepository.getPersonaByAddress(args.personaId)
            requireNotNull(persona) {
                "persona is null"
            }
            personaUiState = personaUiState.copy(
                personaName = persona.displayName.orEmpty(),
                personaAddressTruncated = persona.entityAddress.address.truncatedHash()
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
        val personaName: String = "",
        val personaAddressTruncated: String = ""
    )
}

internal sealed interface CreatePersonaConfirmationEvent : OneOffEvent {
    object FinishPersonaCreation : CreatePersonaConfirmationEvent
}
