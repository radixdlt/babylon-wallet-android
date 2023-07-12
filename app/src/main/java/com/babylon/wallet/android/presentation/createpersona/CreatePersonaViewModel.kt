package com.babylon.wallet.android.presentation.createpersona

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.PersonaEditable
import com.babylon.wallet.android.presentation.common.PersonaEditableImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.model.PersonaDisplayNameFieldWrapper
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.profile.domain.persona.CreatePersonaWithDeviceFactorSourceUseCase
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val createPersonaWithDeviceFactorSourceUseCase: CreatePersonaWithDeviceFactorSourceUseCase,
    private val preferencesManager: PreferencesManager,
    private val deviceSecurityHelper: DeviceSecurityHelper,
) : StateViewModel<CreatePersonaViewModel.CreatePersonaUiState>(),
    OneOffEventHandler<CreatePersonaEvent> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    override fun initialState(): CreatePersonaUiState = CreatePersonaUiState(
        isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
    )

    init {
        viewModelScope.launch {
            personaEditState.collect { s ->
                _state.update {
                    it.copy(
                        anyFieldSelected = s.areThereFieldsSelected,
                        personaDisplayName = s.personaDisplayName,
                        continueButtonEnabled = s.inputValid,
                        currentFields = s.currentFields,
                        fieldsToAdd = s.fieldsToAdd
                    )
                }
            }
        }
        setPersona(null)
    }

    fun onPersonaCreateClick() {
        _state.update { it.copy(loading = true) }
        //TODO persona data
//        viewModelScope.launch {
//            val fields = _state.value.currentFields.map {
//                Network.Persona.Field.init(id = it.id, value = it.value.trim())
//            }
//            val persona = createPersonaWithDeviceFactorSourceUseCase(
//                displayName = _state.value.personaDisplayName.value,
//                fields = fields
//            )
//
//            val personaId = persona.address
//
//            _state.update { it.copy(loading = true) }
//            preferencesManager.markFirstPersonaCreated()
//
//            sendEvent(
//                CreatePersonaEvent.Complete(
//                    personaId = personaId
//                )
//            )
//        }
    }

    data class CreatePersonaUiState(
        val loading: Boolean = false,
        val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
        val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
        val personaDisplayName: PersonaDisplayNameFieldWrapper = PersonaDisplayNameFieldWrapper(),
        val continueButtonEnabled: Boolean = false,
        val anyFieldSelected: Boolean = false,
        val isDeviceSecure: Boolean = false
    ) : UiState
}

internal sealed interface CreatePersonaEvent : OneOffEvent {
    data class Complete(
        val personaId: String
    ) : CreatePersonaEvent
}
