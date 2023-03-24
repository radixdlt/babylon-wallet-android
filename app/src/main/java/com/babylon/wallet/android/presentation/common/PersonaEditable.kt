package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
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

    fun setPersona(persona: Network.Persona?, requiredFieldKinds: List<Network.Persona.Field.Kind> = emptyList())
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

    override fun setPersona(persona: Network.Persona?, requiredFieldKinds: List<Network.Persona.Field.Kind>) {
        val existingPersonaFields =
            persona?.fields?.map {
                PersonaFieldKindWrapper(
                    kind = it.kind,
                    value = it.value,
                    valid = true,
                    required = requiredFieldKinds.contains(it.kind)
                )
            }.orEmpty().toPersistentList()
        val missingFields = requiredFieldKinds.minus(existingPersonaFields.map { it.kind }.toSet())
        val currentFields =
            (existingPersonaFields + missingFields.map { PersonaFieldKindWrapper(it, required = true) }).sortedBy { it.kind.ordinal }
                .toPersistentList()
        _state.update { state ->
            val fieldsToAdd = getFieldsToAdd(currentFields.map { it.kind }.toSet())
            state.copy(
                requiredFieldKinds = requiredFieldKinds.toPersistentList(),
                currentFields = currentFields,
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
            }.map { PersonaFieldKindWrapper(it.kind, required = personaEditState.value.requiredFieldKinds.contains(it.kind)) }
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
                Network.Persona.Field.Kind.GivenName,
                Network.Persona.Field.Kind.FamilyName,
                Network.Persona.Field.Kind.PhoneNumber -> it.copy(valid = it.value.trim().isNotEmpty())
                Network.Persona.Field.Kind.EmailAddress -> it.copy(valid = it.value.trim().isValidEmail())
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
        val requiredFieldKinds = personaEditState.value.requiredFieldKinds
        return (Network.Persona.Field.Kind.values().toSet() - existingFields)
            .sortedBy { it.ordinal }.map { PersonaFieldKindWrapper(kind = it, required = requiredFieldKinds.contains(it)) }
            .toPersistentList()
    }
}

data class PersonaEditLogicState(
    val requiredFieldKinds: ImmutableList<Network.Persona.Field.Kind> = persistentListOf(),
    val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val personaDisplayName: String? = null,
    val areThereFieldsSelected: Boolean = false,
    val inputValid: Boolean = false
)
