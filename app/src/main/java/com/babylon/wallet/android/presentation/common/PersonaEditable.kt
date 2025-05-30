package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.empty
import com.babylon.wallet.android.presentation.model.isValid
import com.babylon.wallet.android.presentation.model.sortOrderInt
import com.radixdlt.sargon.Persona
import com.radixdlt.sargon.PersonaDataEntryId
import com.radixdlt.sargon.extensions.SharedConstants.entityNameMaxLength
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import rdx.works.core.sargon.IdentifiedEntry
import rdx.works.core.sargon.PersonaDataField
import rdx.works.core.sargon.fields

interface PersonaEditable {
    val personaEditState: StateFlow<PersonaEditLogicState>
    fun setPersona(persona: Persona?, requiredFieldKinds: List<PersonaDataField.Kind> = emptyList())
    fun onDeleteField(id: PersonaDataEntryId)
    fun onFieldValueChanged(id: PersonaDataEntryId, value: PersonaDataField)
    fun onDisplayNameChanged(value: String)
    fun onSelectionChanged(id: PersonaDataEntryId, selected: Boolean)
    fun onAddFields()
    fun validateInput()
    fun onFieldFocusChanged(id: PersonaDataEntryId, focused: Boolean)
}

class PersonaEditableImpl : PersonaEditable {

    private val _state: MutableStateFlow<PersonaEditLogicState> = MutableStateFlow(PersonaEditLogicState())

    override val personaEditState: StateFlow<PersonaEditLogicState>
        get() = _state.asStateFlow()

    override fun setPersona(persona: Persona?, requiredFieldKinds: List<PersonaDataField.Kind>) {
        val shouldValidateInput = requiredFieldKinds.isNotEmpty()
        val currentFields = persona?.personaData?.fields?.map {
            PersonaFieldWrapper(
                id = it.uuid,
                entry = IdentifiedEntry.init(it.value, it.id),
                isValid = true,
                required = requiredFieldKinds.contains(it.value.kind)
            )
        }.orEmpty().sortedBy { it.entry.value.sortOrderInt() }.toPersistentList()
        _state.update { state ->
            val fieldsToAdd = getFieldsToAdd(currentFields.map { it.entry.value.kind }.toSet())
            state.copy(
                requiredFieldKinds = requiredFieldKinds.toPersistentList(),
                currentFields = currentFields,
                fieldsToAdd = fieldsToAdd,
                personaDisplayName = PersonaDisplayNameFieldWrapper(
                    persona?.displayName?.value.orEmpty().take(entityNameMaxLength.toInt())
                )
            )
        }
        if (shouldValidateInput) {
            validateInput()
        }
    }

    override fun onDeleteField(id: PersonaDataEntryId) {
        _state.update { s ->
            val updatedFields = s.currentFields.filter {
                it.id.toString() != id.toString()
            }
            s.copy(
                currentFields = updatedFields.toPersistentList(),
                fieldsToAdd = getFieldsToAdd(updatedFields.map { it.entry.value.kind }.toSet())
            )
        }
        validateInput()
    }

    override fun onFieldValueChanged(id: PersonaDataEntryId, value: PersonaDataField) {
        _state.update { s ->
            s.copy(
                currentFields = s.currentFields.mapWhen(predicate = { it.id == id }) {
                    it.copy(entry = it.entry.copy(value = value), wasEdited = true)
                }.toPersistentList()
            )
        }
        validateInput()
    }

    override fun onFieldFocusChanged(id: PersonaDataEntryId, focused: Boolean) {
        _state.update { s ->
            s.copy(
                currentFields = s.currentFields.mapWhen(predicate = { id == it.id }) {
                    if (focused) {
                        it
                    } else {
                        it.copy(shouldDisplayValidationError = it.wasEdited)
                    }
                }.toPersistentList()
            )
        }
        validateInput()
    }

    override fun onDisplayNameChanged(value: String) {
        _state.update { state ->
            state.copy(personaDisplayName = state.personaDisplayName.copy(value = value, wasEdited = true))
        }
        validateInput()
    }

    override fun onSelectionChanged(id: PersonaDataEntryId, selected: Boolean) {
        _state.update { s ->
            val updated = s.fieldsToAdd.map {
                if (it.id == id) {
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
            val existingFields = _state.value.currentFields + _state.value.fieldsToAdd.filter {
                it.selected
            }.map { it.copy(required = personaEditState.value.requiredFieldKinds.contains(it.entry.value.kind)) }
            val fieldsToAdd = getFieldsToAdd(existingFields.map { it.entry.value.kind }.toSet())
            s.copy(
                currentFields = existingFields.sortedBy { it.entry.value.sortOrderInt() }.toPersistentList(),
                fieldsToAdd = fieldsToAdd,
                areThereFieldsSelected = false
            )
        }
        validateInput()
    }

    override fun validateInput() {
        val validatedFields = _state.value.currentFields.map { field ->
            field.copy(isValid = field.entry.value.isValid())
        }
        _state.update { state ->
            state.copy(
                currentFields = validatedFields.toPersistentList()
            )
        }
    }

    private fun getFieldsToAdd(
        existingFieldKinds: Set<PersonaDataField.Kind>
    ): PersistentList<PersonaFieldWrapper> {
        val requiredFieldKinds = personaEditState.value.requiredFieldKinds
        return (PersonaDataField.Kind.supportedKinds.toSet() - existingFieldKinds)
            .sortedBy { it.ordinal }
            .map {
                PersonaFieldWrapper(
                    required = requiredFieldKinds.contains(it),
                    entry = it.empty()
                )
            }
            .toPersistentList()
    }
}

data class PersonaEditLogicState(
    val requiredFieldKinds: ImmutableList<PersonaDataField.Kind> = persistentListOf(),
    val currentFields: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
    val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
    val areThereFieldsSelected: Boolean = false
) {
    val isInputValid: Boolean
        get() = currentFields.all { it.isValid == true } && personaDisplayName.isValid
}
