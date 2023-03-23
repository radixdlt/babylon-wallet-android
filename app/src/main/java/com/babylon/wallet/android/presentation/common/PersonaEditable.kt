package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.domain.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.utils.isValidEmail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.profile.data.model.pernetwork.Network

interface PersonaEditable {

    val personaEditState: StateFlow<PersonaEditLogicState>

    fun setPersona(persona: Network.Persona?)
    fun onDeleteField(kind: Network.Persona.Field.Kind)
    fun onFieldValueChanged(kind: Network.Persona.Field.Kind, value: String)
    fun onDisplayNameChanged(value: String)
    fun onSelectionChanged(kind: Network.Persona.Field.Kind, selected: Boolean)
    fun onAddFields()
    fun validateInput()
}

class PersonaEditableImpl : PersonaEditable {

    private val _state: MutableStateFlow<PersonaEditLogicState> = MutableStateFlow(PersonaEditLogicState())

    override val personaEditState: StateFlow<PersonaEditLogicState>
        get() = _state.asStateFlow()

    override fun setPersona(persona: Network.Persona?) {
        val existingFields =
            persona?.fields?.map { PersonaFieldKindWrapper(it.kind, value = it.value, valid = true) }.orEmpty().toPersistentList()
        _state.update { state ->
            val fieldsToAdd = getFieldsToAdd(existingFields.map { it.kind }.toSet())
            state.copy(
                currentFields = existingFields,
                fieldsToAdd = fieldsToAdd,
                personaDisplayName = persona?.displayName
            )
        }
    }

    override fun onDeleteField(kind: Network.Persona.Field.Kind) {
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

    override fun onFieldValueChanged(kind: Network.Persona.Field.Kind, value: String) {
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

    override fun onDisplayNameChanged(value: String) {
        _state.update { it.copy(personaDisplayName = value) }
        validateInput()
    }

    override fun onSelectionChanged(kind: Network.Persona.Field.Kind, selected: Boolean) {
        _state.update { s ->
            val updated = s.fieldsToAdd.map {
                if (it.kind == kind) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }.toPersistentList()
            s.copy(fieldsToAdd = updated, areThereFieldsSelected = updated.any { it.selected })
        }
    }

    override fun onAddFields() {
        _state.update { s ->
            val existingFields = _state.value.currentFields.map { field ->
                PersonaFieldKindWrapper(field.kind, value = field.value)
            } + _state.value.fieldsToAdd.filter {
                it.selected
            }.map { PersonaFieldKindWrapper(it.kind) }
            val fieldsToAdd = getFieldsToAdd(existingFields.map { it.kind }.toSet())
            s.copy(
                currentFields = existingFields.sortedBy { it.kind.ordinal }.toPersistentList(),
                fieldsToAdd = fieldsToAdd,
                areThereFieldsSelected = false
            )
        }
        validateInput()
    }

    override fun validateInput() {
        val validatedFields = _state.value.currentFields.map {
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
                inputValid = validatedFields.all { it.valid == true } && state.personaDisplayName?.trim()
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

data class PersonaEditLogicState(
    val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val personaDisplayName: String? = null,
    val areThereFieldsSelected: Boolean = false,
    val inputValid: Boolean = false
)
