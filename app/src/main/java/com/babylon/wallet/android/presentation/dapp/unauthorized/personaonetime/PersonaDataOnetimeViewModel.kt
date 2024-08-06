package com.babylon.wallet.android.presentation.dapp.unauthorized.personaonetime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
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
import kotlinx.collections.immutable.toPersistentList
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

    private val args = PersonaDataOnetimeUnauthorizedArgs(savedStateHandle)

    override fun initialState(): PersonaDataOnetimeUiState {
        return PersonaDataOnetimeUiState(showBack = args.showBack)
    }

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.map { it.activePersonasOnCurrentNetwork }.collect { personas ->
                _state.update { state ->
                    val existingPersonas = state.personaListToDisplay
                    state.copy(
                        personaListToDisplay = personas.map { persona ->
                            val existingPersona = existingPersonas.firstOrNull { it.persona.address == persona.address }
                            PersonaUiModel(
                                persona,
                                requiredPersonaFields = args.requiredPersonaFields,
                                selected = existingPersona?.selected ?: false
                            )
                        }.toImmutableList()
                    )
                }
            }
        }
    }

    fun onSelectPersona(persona: Persona) {
        val updatedPersonas = state.value.personaListToDisplay.map {
            it.copy(selected = it.persona.address == persona.address)
        }.toPersistentList()
        val selectedPersonaHasAllTheData = updatedPersonas.any { it.selected && it.missingFieldKinds().isEmpty() }
        _state.update {
            it.copy(
                personaListToDisplay = updatedPersonas,
                continueButtonEnabled = selectedPersonaHasAllTheData
            )
        }
    }

    fun onEditClick(persona: Persona) {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.OnEditPersona(persona.address, args.requiredPersonaFields))
        }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonaDataOnetimeEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }
}

sealed interface PersonaDataOnetimeEvent : OneOffEvent {
    data class OnEditPersona(
        val personaAddress: IdentityAddress,
        val requiredPersonaFields: RequiredPersonaFields? = null
    ) : PersonaDataOnetimeEvent

    data class CreatePersona(val firstPersonaCreated: Boolean) : PersonaDataOnetimeEvent
}

data class PersonaDataOnetimeUiState(
    val personaListToDisplay: ImmutableList<PersonaUiModel> = persistentListOf(),
    val continueButtonEnabled: Boolean = false,
    val showBack: Boolean = false
) : UiState
