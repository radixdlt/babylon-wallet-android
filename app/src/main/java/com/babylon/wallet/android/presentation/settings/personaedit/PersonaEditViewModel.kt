package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.isValidEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
) : ViewModel(), OneOffEventHandler<PersonaEditEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaEditScreenArgs(savedStateHandle)

    private val _state: MutableStateFlow<PersonaEditUiState> =
        MutableStateFlow(PersonaEditUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            personaRepository.getPersonaByAddressFlow(args.personaAddress).collect { persona ->
                _state.update { state ->
                    val existingFields =
                        persona.fields.map { PersonaFieldKindWrapper(it.kind, value = it.value, valid = true) }
                            .toPersistentList()
                    state.copy(
                        persona = persona,
                        currentFields = existingFields,
                        fieldsToAdd = getFieldsToAdd(existingFields.map { it.kind }.toSet()),
                        personaDisplayName = persona.displayName
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
                    persona.copy(displayName = state.value.personaDisplayName?.trim().orEmpty(), fields = fields)
                updatePersonaUseCase(updatedPersona)
                sendEvent(PersonaEditEvent.PersonaSaved)
            }
        }
    }

    fun onDeleteField(kind: Network.Persona.Field.Kind) {
        _state.update { s ->
            val updatedFields = s.currentFields.filter {
                it.kind != kind
            }
            s.copy(
                currentFields = updatedFields.toPersistentList(),
                fieldsToAdd = getFieldsToAdd(updatedFields.map { it.kind }.toSet())
            )
        }
        validateInput()
    }

    fun onValueChanged(kind: Network.Persona.Field.Kind, value: String) {
        _state.update { s ->
            s.copy(
                currentFields = s.currentFields.map {
                    if (it.kind == kind) {
                        it.copy(value = value)
                    } else {
                        it
                    }
                }.toPersistentList()
            )
        }
        validateInput()
    }

    fun onDisplayNameChanged(value: String) {
        _state.update { it.copy(personaDisplayName = value) }
        validateInput()
    }

    fun onSelectionChanged(kind: Network.Persona.Field.Kind, selected: Boolean) {
        _state.update { s ->
            val updated = s.fieldsToAdd.map {
                if (it.kind == kind) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }.toPersistentList()
            s.copy(fieldsToAdd = updated, addButtonEnabled = updated.any { it.selected })
        }
    }

    fun onAddFields() {
        _state.update { s ->
            val existingFields = state.value.currentFields.map { field ->
                PersonaFieldKindWrapper(field.kind, value = field.value)
            } + state.value.fieldsToAdd.filter {
                it.selected
            }.map { PersonaFieldKindWrapper(it.kind) }
            s.copy(
                currentFields = existingFields.sortedBy { it.kind.ordinal }.toPersistentList(),
                fieldsToAdd = getFieldsToAdd(existingFields.map { it.kind }.toSet()),
                addButtonEnabled = false
            )
        }
        validateInput()
    }

    private fun validateInput() {
        val validatedFields = state.value.currentFields.map {
            when (it.kind) {
                Network.Persona.Field.Kind.FirstName,
                Network.Persona.Field.Kind.LastName,
                Network.Persona.Field.Kind.PersonalIdentificationNumber,
                Network.Persona.Field.Kind.ZipCode -> it.copy(valid = it.value.trim().isNotEmpty())
                Network.Persona.Field.Kind.Email -> it.copy(valid = it.value.trim().isValidEmail())
            }
        }
        _state.update { state ->
            state.copy(
                currentFields = validatedFields.toPersistentList(),
                saveButtonEnabled = validatedFields.all { it.valid == true } && state.personaDisplayName?.trim()
                    ?.isNotEmpty() == true
            )
        }
    }

    private fun getFieldsToAdd(
        existingFields: Set<Network.Persona.Field.Kind>
    ): PersistentList<PersonaFieldKindWrapper> {
        return (
            Network.Persona.Field.Kind.values()
                .toSet() - existingFields
            ).sortedBy { it.ordinal }.map { PersonaFieldKindWrapper(kind = it) }.toPersistentList()
    }
}

sealed interface PersonaEditEvent : OneOffEvent {
    object PersonaSaved : PersonaEditEvent
}

data class PersonaFieldKindWrapper(
    val kind: Network.Persona.Field.Kind,
    val selected: Boolean = false,
    val value: String = "",
    val valid: Boolean? = null
)

data class PersonaEditUiState(
    val persona: Network.Persona? = null,
    val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val personaDisplayName: String? = null,
    val addButtonEnabled: Boolean = false,
    val saveButtonEnabled: Boolean = false
)
