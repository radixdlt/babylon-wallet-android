package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.domain.model.RequiredPersonaFields
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.activePersonaOnCurrentNetwork
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class PersonaDataOngoingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<PersonaDataOngoingUiState>(), OneOffEventHandler<PersonaDataOngoingEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDataOngoingPermissionArgs(savedStateHandle)

    override fun initialState(): PersonaDataOngoingUiState {
        return PersonaDataOngoingUiState(showBack = args.showBack)
    }

    init {
        viewModelScope.launch {
            getProfileUseCase.flow.mapNotNull { it.activePersonaOnCurrentNetwork(args.personaId) }.collect { persona ->
                val uiModel = PersonaUiModel(persona, requiredPersonaFields = args.requiredPersonaFields)
                _state.update {
                    it.copy(
                        persona = uiModel,
                        continueButtonEnabled = uiModel.missingFieldKinds().isEmpty()
                    )
                }
            }
        }
    }

    fun onEditClick(persona: Persona) {
        viewModelScope.launch {
            sendEvent(PersonaDataOngoingEvent.OnEditPersona(persona.address, args.requiredPersonaFields))
        }
    }
}

sealed interface PersonaDataOngoingEvent : OneOffEvent {
    data class OnEditPersona(
        val personaAddress: IdentityAddress,
        val requiredPersonaFields: RequiredPersonaFields
    ) : PersonaDataOngoingEvent
}

data class PersonaDataOngoingUiState(
    val persona: PersonaUiModel? = null,
    val continueButtonEnabled: Boolean = false,
    val showBack: Boolean = false
) : UiState
