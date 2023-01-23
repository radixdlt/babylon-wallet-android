package com.babylon.wallet.android.presentation.createpersona

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val createPersonaUseCase: CreatePersonaUseCase,
    deviceSecurityHelper: DeviceSecurityHelper,
) : ViewModel(), OneOffEventHandler<CreatePersonaEvent> by OneOffEventHandlerImpl() {

    var state by mutableStateOf(
        CreatePersonaUiState(
            isDeviceSecure = deviceSecurityHelper.isDeviceSecure()
        )
    )
        private set

    fun onPersonaNameChange(personaName: String) {
        state = state.copy(
            personaName = personaName.take(PERSONA_NAME_MAX_LENGTH),
            buttonEnabled = personaName.trim().isNotEmpty()
        )
    }

    fun onPersonaCreateClick() {
        state = state.copy(
            loading = true
        )
        viewModelScope.launch {
            val persona = createPersonaUseCase(
                displayName = state.personaName,
                fields = emptyList()
            )

            val personaId = persona.entityAddress.address

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
        val personaName: String = "",
        val buttonEnabled: Boolean = false,
        val isDeviceSecure: Boolean = false
    )

    companion object {
        private const val PERSONA_NAME_MAX_LENGTH = 20
    }
}

internal sealed interface CreatePersonaEvent : OneOffEvent {
    data class Complete(
        val personaId: String
    ) : CreatePersonaEvent
}
