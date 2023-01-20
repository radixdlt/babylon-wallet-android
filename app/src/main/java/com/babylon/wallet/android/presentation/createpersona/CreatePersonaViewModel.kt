package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.DeviceSecurityHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.domain.CreatePersonaUseCase
import javax.inject.Inject

@HiltViewModel
class CreatePersonaViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createPersonaUseCase: CreatePersonaUseCase,
    deviceSecurityHelper: DeviceSecurityHelper,
) : ViewModel(), OneOffEventHandler<CreatePersonaEvent> by OneOffEventHandlerImpl() {

    val personaName = savedStateHandle.getStateFlow(PERSONA_NAME, "")
    val buttonEnabled = savedStateHandle.getStateFlow(CREATE_PERSONA_BUTTON_ENABLED, false)

    var state by mutableStateOf(
        CreatePersonaState(
            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
        )
    )
        private set

    fun onPersonaNameChange(personaName: String) {
        savedStateHandle[PERSONA_NAME] = personaName.take(PERSONA_NAME_MAX_LENGTH)
        savedStateHandle[CREATE_PERSONA_BUTTON_ENABLED] = personaName.trim().isNotEmpty()
    }

    fun onPersonaCreateClick() {
        state = state.copy(
            loading = true
        )
        viewModelScope.launch {
            val personaName = personaName.value.trim()
            val persona = createPersonaUseCase(
                displayName = personaName,
                fields = emptyList()
            )

            val personaId = persona.entityAddress.address

            state = state.copy(
                loading = true,
                personaId = personaId,
                personaName = personaName
            )

            sendEvent(
                CreatePersonaEvent.Complete(
                    personaId = personaId
                )
            )
        }
    }

    data class CreatePersonaState(
        val loading: Boolean = false,
        val personaId: String = "",
        val personaName: String = "",
        val isDeviceSecure: Boolean = false
    )

    companion object {
        private const val PERSONA_NAME_MAX_LENGTH = 20
        private const val PERSONA_NAME = "persona_name"
        private const val CREATE_PERSONA_BUTTON_ENABLED = "create_persona_button_enabled"
    }
}

internal sealed interface CreatePersonaEvent : OneOffEvent {
    data class Complete(
        val personaId: String
    ) : CreatePersonaEvent
}
