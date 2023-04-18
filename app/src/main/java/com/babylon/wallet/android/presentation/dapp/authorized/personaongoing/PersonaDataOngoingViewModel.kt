package com.babylon.wallet.android.presentation.dapp.authorized.personaongoing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.presentation.dapp.authorized.selectpersona.PersonaUiModel
import com.babylon.wallet.android.presentation.model.encodeToString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personaOnCurrentNetworkFlow
import javax.inject.Inject

@HiltViewModel
class PersonaDataOngoingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProfileUseCase: GetProfileUseCase
) : StateViewModel<PersonaDataOngoingUiState>(), OneOffEventHandler<PersonaDataOngoingEvent> by OneOffEventHandlerImpl() {

    private val args = PersonaDataOngoingPermissionArgs(savedStateHandle)

    override fun initialState(): PersonaDataOngoingUiState {
        return PersonaDataOngoingUiState()
    }

    init {
        viewModelScope.launch {
            getProfileUseCase.personaOnCurrentNetworkFlow(args.personaId).collect { persona ->
                val uiModel = PersonaUiModel(persona, requiredFieldKinds = args.requiredFields.toList())
                _state.update {
                    it.copy(
                        persona = uiModel,
                        continueButtonEnabled = uiModel.missingFieldKinds().isEmpty()
                    )
                }
            }
        }
    }

    fun onEditClick(personaAddress: String) {
        viewModelScope.launch {
            sendEvent(PersonaDataOngoingEvent.OnEditPersona(personaAddress, args.requiredFields.toList().encodeToString()))
        }
    }
}

sealed interface PersonaDataOngoingEvent : OneOffEvent {
    data class OnEditPersona(val personaAddress: String, val requiredFieldsEncoded: String? = null) : PersonaDataOngoingEvent
}

data class PersonaDataOngoingUiState(
    val persona: PersonaUiModel? = null,
    val continueButtonEnabled: Boolean = false
) : UiState
