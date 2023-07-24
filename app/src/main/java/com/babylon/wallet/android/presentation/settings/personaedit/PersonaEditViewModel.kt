package com.babylon.wallet.android.presentation.settings.personaedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.PersonaEditable
import com.babylon.wallet.android.presentation.common.PersonaEditableImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldWrapper
import com.babylon.wallet.android.presentation.model.toPersonaData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.PersonaData
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.persona.UpdatePersonaUseCase
import rdx.works.profile.domain.personaOnCurrentNetworkFlow
import javax.inject.Inject

@HiltViewModel
class PersonaEditViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updatePersonaUseCase: UpdatePersonaUseCase,
    savedStateHandle: SavedStateHandle
) : StateViewModel<PersonaEditUiState>(),
    OneOffEventHandler<PersonaEditEvent> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    private val args = PersonaEditScreenArgs(savedStateHandle)

    override fun initialState() = PersonaEditUiState()

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
                        wasEdited = s.currentFields.any { it.wasEdited } || state.currentFields.size != s.currentFields.size,
                        missingFields = missingPersonaFieldKinds()
                    )
                }
            }
        }
        viewModelScope.launch {
            getProfileUseCase.personaOnCurrentNetworkFlow(args.personaAddress).collect { persona ->
                setPersona(persona = persona, requiredFieldKinds = args.requiredPersonaFields?.fields?.map { it.kind }.orEmpty())
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

    private fun missingPersonaFieldKinds(): PersistentList<PersonaData.PersonaDataField.Kind> {
        return args.requiredPersonaFields?.fields?.map {
            it.kind
        }?.toSet()?.minus(state.value.currentFields.map { it.entry.value.kind }.toSet()).orEmpty().toPersistentList()
    }

    fun onSave() {
        viewModelScope.launch {
            state.value.persona?.let { persona ->
                val personaData = state.value.currentFields.toPersonaData()
                val updatedPersona =
                    persona.copy(displayName = state.value.personaDisplayName.value.trim(), personaData = personaData)
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
    val currentFields: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
    val fieldsToAdd: ImmutableList<PersonaFieldWrapper> = persistentListOf(),
    val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
    val addFieldButtonEnabled: Boolean = false,
    val saveButtonEnabled: Boolean = false,
    val dappContextEdit: Boolean = false,
    val wasEdited: Boolean = false,
    val missingFields: ImmutableList<PersonaData.PersonaDataField.Kind> = persistentListOf()
) : UiState
