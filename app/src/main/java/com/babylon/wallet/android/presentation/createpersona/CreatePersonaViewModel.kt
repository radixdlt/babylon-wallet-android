package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.PersonaEditable
import com.babylon.wallet.android.presentation.common.PersonaEditableImpl
import com.babylon.wallet.android.presentation.model.PersonaFieldKindWrapper
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.CreatePersonaUseCase
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val createPersonaUseCase: CreatePersonaUseCase,
    deviceSecurityHelper: DeviceSecurityHelper,
) : ViewModel(),
    OneOffEventHandler<CreatePersonaEvent> by OneOffEventHandlerImpl(),
    PersonaEditable by PersonaEditableImpl() {

    var state by mutableStateOf(
        CreatePersonaUiState(
            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
        )
    )
        private set

    init {
        viewModelScope.launch {
            personaEditState.collect { s ->
                state = state.copy(
                    anyFieldSelected = s.areThereFieldsSelected,
                    personaDisplayName = s.personaDisplayName.orEmpty(),
                    continueButtonEnabled = s.inputValid,
                    currentFields = s.currentFields,
                    fieldsToAdd = s.fieldsToAdd
                )
            }
        }
        setPersona(null)
    }

    fun onPersonaCreateClick() {
        state = state.copy(
            loading = true
        )
        viewModelScope.launch {
            val fields = state.currentFields.map {
                Network.Persona.Field.init(kind = it.kind, value = it.value.trim())
            }
            val persona = createPersonaUseCase(
                displayName = state.personaDisplayName,
                fields = fields
            )

            val personaId = persona.address

            state = state.copy(
                loading = true
            )

            sendEvent(
                CreatePersonaEvent.Complete(
                    personaId = personaId
                )
            )
        }
    }

    data class CreatePersonaUiState(
        val loading: Boolean = false,
        val currentFields: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
        val fieldsToAdd: ImmutableList<PersonaFieldKindWrapper> = persistentListOf(),
        val personaDisplayName: String = "",
        val continueButtonEnabled: Boolean = false,
        val anyFieldSelected: Boolean = false,
        val isDeviceSecure: Boolean = false
    )
}

internal sealed interface CreatePersonaEvent : OneOffEvent {
    data class Complete(
        val personaId: String
    ) : CreatePersonaEvent
}
