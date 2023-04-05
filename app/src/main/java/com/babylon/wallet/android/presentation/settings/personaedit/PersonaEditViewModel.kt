package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.PersonaEditable
import com.babylon.wallet.android.presentation.common.PersonaEditableImpl
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.PersonaRepository
import rdx.works.profile.domain.UpdatePersonaUseCase
import javax.inject.Inject

@HiltViewModel
class PersonaEditViewModel @Inject constructor(
    private val personaRepository: PersonaRepository,
    private val updatePersonaUseCase: UpdatePersonaUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel(),
    OneOffEventHandler<PersonaEditEvent> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    private val args = PersonaEditScreenArgs(savedStateHandle)

    private val _state: MutableStateFlow<PersonaEditUiState> =
        MutableStateFlow(PersonaEditUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            personaEditState.collect { s ->
                _state.update { state ->
                    state.copy(
                        saveButtonEnabled = s.inputValid,
                        personaDisplayName = s.personaDisplayName,
                        addFieldButtonEnabled = s.areThereFieldsSelected,
                        currentFields = s.currentFields,
                        fieldsToAdd = s.fieldsToAdd,
                        dappContextEdit = s.requiredFieldKinds.isNotEmpty(),
                        wasEdited = s.currentFields.any { it.wasEdited } || state.currentFields.size != s.currentFields.size
                    )
                }
            }
        }
        viewModelScope.launch {
            personaRepository.getPersonaByAddressFlow(args.personaAddress).collect { persona ->
                setPersona(persona = persona, requiredFieldKinds = args.requiredFields.toList())
                _state.update { state ->
                    state.copy(
                        persona = persona,
                        personaDisplayName = PersonaDisplayNameFieldWrapper(
                            value = persona.displayName
                        )
                    )
                }
            }
        }
    }

    fun onSave() {
        viewModelScope.launch {
            state.value.persona?.let { persona ->
                val fields = state.value.currentFields.map {
                    Network.Persona.Field.init(kind = it.kind, value = it.value.trim())
                }
                val updatedPersona =
                    persona.copy(displayName = state.value.personaDisplayName.value.trim(), fields = fields)
                updatePersonaUseCase(updatedPersona)
                sendEvent(PersonaEditEvent.PersonaSaved)
            }
        }
    }
}

sealed interface PersonaEditEvent : OneOffEvent {
    object PersonaSaved : PersonaEditEvent
}

data class PersonaEditUiState(
    val persona: Network.Persona? = null,
    val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
    val addFieldButtonEnabled: Boolean = false,
    val saveButtonEnabled: Boolean = false,
    val dappContextEdit: Boolean = false,
    val wasEdited: Boolean = false
)
