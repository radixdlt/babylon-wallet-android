package com.babylon.wallet.android.presentation.dapp.authorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class PersonaDataOnetimeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager
) : StateViewModel<PersonaDataOnetimeUiState>(), OneOffEventHandler<PersonaDataOnetimeEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDataOnetimeArgs(savedStateHandle)

    override fun initialState(): PersonaDataOnetimeUiState {
        return PersonaDataOnetimeUiState(showBack = args.showBack)
    }

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.map { it.activePersonasOnCurrentNetwork }.collect { personas ->
                // The persona chosen stays chosen when the profile changes (the person added the
                // missing data, say), and whether it can be shared is decided again.
                _state.update { state -> state.showing(personas, state.selectedPersonaAddress) }
            }
        }
    }

    fun onSelectPersona(persona: Persona) {
        // Kept even when the list has not loaded yet: it is shown selected once it does.
        _state.update { state -> state.showing(state.personaListToDisplay.map { it.persona }, persona.address) }
    }

    /**
     * The state showing [personas], with the one at [selected] chosen. Only a persona with all the data
     * asked for can be shared, so Continue is enabled only for one: the others show what is missing.
     */
    private fun PersonaDataOnetimeUiState.showing(personas: List<Persona>, selected: IdentityAddress?): PersonaDataOnetimeUiState {
        val list = personas.map {
            PersonaUiModel(it, selected = it.address == selected, requiredPersonaFields = args.requiredPersonaFields)
        }.toImmutableList()
        return copy(
            personaListToDisplay = list,
            selectedPersonaAddress = selected,
            continueButtonEnabled = list.any { it.selected && it.hasRequiredData }
        )
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    fun onEditClick(persona: Persona) {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.OnEditPersona(persona, args.requiredPersonaFields))
        }
    }
}

sealed interface PersonaDataOnetimeEvent : OneOffEvent {
    data class OnEditPersona(
        val persona: Persona,
        val requiredPersonaFields: RequiredPersonaFields
    ) : PersonaDataOnetimeEvent
    data class CreatePersona(val firstPersonaCreated: Boolean) : PersonaDataOnetimeEvent
}

data class PersonaDataOnetimeUiState(
    val personaListToDisplay: ImmutableList<PersonaUiModel> = persistentListOf(),
    // The persona chosen, kept apart from the list: a choice made before the list loads is not lost.
    val selectedPersonaAddress: IdentityAddress? = null,
    val continueButtonEnabled: Boolean = false,
    val showBack: Boolean = false
) : UiState {
    fun selectedPersona(): Persona {
        return personaListToDisplay.first { it.selected }.persona
    }
}
