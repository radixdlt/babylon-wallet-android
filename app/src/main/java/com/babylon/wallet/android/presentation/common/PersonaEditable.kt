package com.babylon.wallet.android.presentation.common

import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rdx.works.core.mapWhen
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaDataEntryID

interface PersonaEditable {

    val personaEditState: StateFlow<PersonaEditLogicState>

    fun setPersona(persona: Network.Persona?, requiredFieldIDS: List<PersonaDataEntryID> = emptyList())
    fun onDeleteField(id: PersonaDataEntryID)
    fun onFieldValueChanged(id: PersonaDataEntryID, value: String)
    fun onDisplayNameChanged(value: String)
    fun onSelectionChanged(id: PersonaDataEntryID, selected: Boolean)
    fun onAddFields()
    fun validateInput()
    fun onFieldFocusChanged(id: PersonaDataEntryID, focused: Boolean)
    fun onPersonaDisplayNameFieldFocusChanged(focused: Boolean)
}

class PersonaEditableImpl : PersonaEditable {

    private val _state: MutableStateFlow<PersonaEditLogicState> = MutableStateFlow(PersonaEditLogicState())

    override val personaEditState: StateFlow<PersonaEditLogicState>
        get() = _state.asStateFlow()

    override fun setPersona(persona: Network.Persona?, requiredFieldIDS: List<PersonaDataEntryID>) {
        //TODO persona data
//        val shouldValidateInput = requiredFieldIDS.isNotEmpty()
//        val existingPersonaFields =
//            persona?.personaData?.allFieldIds()?.map {
//                PersonaFieldKindWrapper(
//                    id = it.id,
//                    value = it.value,
//                    valid = true,
//                    required = requiredFieldIDS.contains(it.id)
//                )
//            }.orEmpty().toPersistentList()
//        val missingFields = requiredFieldIDS.minus(existingPersonaFields.map { it.id }.toSet())
//        val currentFields =
//            (
//                existingPersonaFields + missingFields.map {
//                    PersonaFieldKindWrapper(
//                        it,
//                        required = true,
//                        shouldDisplayValidationError = true,
//                        wasEdited = true
//                    )
//                }
//                ).sortedBy { it.id.ordinal }
//                .toPersistentList()
//        _state.update { state ->
//            val fieldsToAdd = getFieldsToAdd(currentFields.map { it.id }.toSet())
//            state.copy(
//                requiredFieldIDS = requiredFieldIDS.toPersistentList(),
//                currentFields = currentFields,
//                fieldsToAdd = fieldsToAdd,
//                personaDisplayName = PersonaDisplayNameFieldWrapper(persona?.displayName.orEmpty())
//            )
//        }
//        if (shouldValidateInput) {
//            validateInput()
//        }
    }

    override fun onDeleteField(id: PersonaDataEntryID) {
        _state.update { s ->
            val updatedFields = s.currentFields.filter {
                it.id != id
            }
            s.copy(
                currentFields = updatedFields.toPersistentList(),
                fieldsToAdd = getFieldsToAdd(updatedFields.map { it.id }.toSet())
            )
        }
        validateInput()
    }

    override fun onFieldValueChanged(id: PersonaDataEntryID, value: String) {
        _state.update { s ->
            s.copy(
                currentFields = s.currentFields.mapWhen(predicate = { it.id == id }) {
                    it.copy(value = value, wasEdited = true)
                }.toPersistentList()
            )
        }
        validateInput()
    }

    override fun onFieldFocusChanged(id: PersonaDataEntryID, focused: Boolean) {
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

    override fun onPersonaDisplayNameFieldFocusChanged(focused: Boolean) {
        if (!focused) {
            _state.update { s ->
                val displayName = s.personaDisplayName
                s.copy(
                    personaDisplayName = displayName.copy(shouldDisplayValidationError = displayName.wasEdited)
                )
            }
            validateInput()
        }
    }

    override fun onDisplayNameChanged(value: String) {
        _state.update { state ->
            state.copy(personaDisplayName = state.personaDisplayName.copy(value = value, wasEdited = true))
        }
        validateInput()
    }

    override fun onSelectionChanged(id: PersonaDataEntryID, selected: Boolean) {
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
        //TODO persona data
//        _state.update { s ->
//            val existingFields = _state.value.currentFields + _state.value.fieldsToAdd.filter {
//                it.selected
//            }.map { PersonaFieldKindWrapper(it.id, required = personaEditState.value.requiredFieldIDS.contains(it.id)) }
//            val fieldsToAdd = getFieldsToAdd(existingFields.map { it.id }.toSet())
//            s.copy(
//                currentFields = existingFields.sortedBy { it.id.ordinal }.toPersistentList(),
//                fieldsToAdd = fieldsToAdd,
//                areThereFieldsSelected = false
//            )
//        }
        validateInput()
    }

    override fun validateInput() {
        //TODO persona data
//        val validatedFields = _state.value.currentFields.map {
//            when (it.id) {
//                Network.Persona.Field.ID.GivenName,
//                Network.Persona.Field.ID.FamilyName,
//                Network.Persona.Field.ID.PhoneNumber -> {
//                    it.copy(valid = it.value.trim().isNotEmpty())
//                }
//                Network.Persona.Field.ID.EmailAddress -> it.copy(valid = it.value.trim().isValidEmail())
//            }
//        }
//        _state.update { state ->
//            state.copy(
//                currentFields = validatedFields.toPersistentList(),
//                inputValid = validatedFields.all { it.valid == true } && state.personaDisplayName.value.trim().isNotEmpty(),
//                personaDisplayName = state.personaDisplayName.copy(valid = state.personaDisplayName.value.trim().isNotEmpty())
//            )
//        }
    }

    private fun getFieldsToAdd(
        existingFields: Set<PersonaDataEntryID>
    ): PersistentList<PersonaFieldKindWrapper> {
        //TODO persona data
//        val requiredFieldKinds = personaEditState.value.requiredFieldIDS
//        return (Network.Persona.Field.ID.values().toSet() - existingFields)
//            .sortedBy { it.ordinal }.map { PersonaFieldKindWrapper(id = it, required = requiredFieldKinds.contains(it)) }
//            .toPersistentList()
        return persistentListOf()
    }
}

data class PersonaEditLogicState(
    val requiredFieldIDS: ImmutableList<PersonaDataEntryID> = persistentListOf(),
    val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
    val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
    val areThereFieldsSelected: Boolean = false,
    val inputValid: Boolean = false
)
