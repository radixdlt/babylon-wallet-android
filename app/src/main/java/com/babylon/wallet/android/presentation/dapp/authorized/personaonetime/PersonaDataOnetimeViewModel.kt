package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.presentation.common.BaseViewModel
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.encodeToString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.PersonaRepository
import javax.inject.Inject

@HiltViewModel
class PersonaDataOnetimeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personaRepository: PersonaRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel<PersonaDataOnetimeUiState>(), OneOffEventHandler<PersonaDataOnetimeEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDataOnetimeArgs(savedStateHandle)

    override fun initialState(): PersonaDataOnetimeUiState {
        return PersonaDataOnetimeUiState()
    }

    init {
        viewModelScope.launch {
            personaRepository.personas.collect { personas ->
                _state.update { state ->
                    state.copy(
                        personaListToDisplay = personas.map {
                            PersonaUiModel(it, requiredFieldKinds = args.requiredFields.toList())
                        }.toImmutableList()
                    )
                }
            }
        }
    }

    fun onSelectPersona(persona: Network.Persona) {
        val updatedPersonas = state.value.personaListToDisplay.map {
            it.copy(selected = it.persona.address == persona.address)
        }.toPersistentList()
        _state.update { it.copy(personaListToDisplay = updatedPersonas, continueButtonEnabled = true) }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    fun onEditClick(personaAddress: String) {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.OnEditPersona(personaAddress, args.requiredFields.toList().encodeToString()))
        }
    }
}

sealed interface PersonaDataOnetimeEvent : OneOffEvent {
    data class OnEditPersona(val personaAddress: String, val requiredFieldsEncoded: String? = null) : PersonaDataOnetimeEvent
    data class CreatePersona(val firstPersonaCreated: Boolean) : PersonaDataOnetimeEvent
}

data class PersonaDataOnetimeUiState(
    val personaListToDisplay: ImmutableList<PersonaUiModel> = persistentListOf(),
    val continueButtonEnabled: Boolean = false
) : UiState {
    fun selectedPersona(): Network.Persona {
        return personaListToDisplay.first { it.selected }.persona
    }
}
