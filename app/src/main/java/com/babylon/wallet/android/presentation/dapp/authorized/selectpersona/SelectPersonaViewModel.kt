package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.LAST_USED_PERSONA_DATE_FORMAT
import com.babylon.wallet.android.utils.fromISO8601String
import com.babylon.wallet.android.utils.toEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.model.pernetwork.Network.AuthorizedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personasOnCurrentNetwork
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SelectPersonaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager,
    incomingRequestRepository: IncomingRequestRepository
) : StateViewModel<SelectPersonaUiState>(), OneOffEventHandler<DAppSelectPersonaEvent> by OneOffEventHandlerImpl() {

    private val args = SelectPersonaArgs(savedStateHandle)

    private val authorizedRequest = incomingRequestRepository.getAuthorizedRequest(
        args.requestId
    )

    private var authorizedDapp: Network.AuthorizedDapp? = null

    override fun initialState(): SelectPersonaUiState = SelectPersonaUiState()

    init {
        viewModelScope.launch {
            authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                authorizedRequest.requestMetadata.dAppDefinitionAddress
            )
            val allAuthorizedPersonas = authorizedDapp?.referencesToAuthorizedPersonas
            _state.update { state ->
                val personas = generatePersonasListForDisplay(
                    allAuthorizedPersonas,
                    state.personaListToDisplay
                ).toPersistentList()
                val selected = personas.any { it.selected }
                state.copy(
                    firstTimeLogin = authorizedDapp == null,
                    personaListToDisplay = personas,
                    continueButtonEnabled = selected,
                    isLoading = false
                )
            }
        }
        observePersonas()
    }

    private fun observePersonas() {
        viewModelScope.launch {
            getProfileUseCase.personasOnCurrentNetwork.collect { personas ->
                authorizedDapp = dAppConnectionRepository.getAuthorizedDapp(
                    authorizedRequest.requestMetadata.dAppDefinitionAddress
                )
                val allAuthorizedPersonas = authorizedDapp?.referencesToAuthorizedPersonas
                _state.update { state ->
                    val personasListForDisplay = generatePersonasListForDisplay(
                        allAuthorizedPersonas,
                        personas.map { it.toUiModel() }
                    )
                    val selected = personasListForDisplay.any { it.selected }
                    state.copy(
                        personaListToDisplay = personasListForDisplay.toPersistentList(),
                        continueButtonEnabled = selected
                    )
                }
            }
        }
    }

    private suspend fun generatePersonasListForDisplay(
        allAuthorizedPersonas: List<AuthorizedPersonaSimple>?,
        profilePersonas: List<PersonaUiModel>
    ): List<PersonaUiModel> {
        val updatedPersonas = profilePersonas.map { personaUiModel ->
            val matchingAuthorizedPersona = allAuthorizedPersonas?.firstOrNull {
                personaUiModel.persona.address == it.identityAddress
            }
            if (matchingAuthorizedPersona != null) {
                val localDateTime = matchingAuthorizedPersona.lastUsedOn.fromISO8601String()
                personaUiModel.copy(
                    lastUsedOn = localDateTime
                        ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
                    lastUsedOnTimestamp = localDateTime?.toEpochMillis() ?: 0
                )
            } else {
                personaUiModel
            }
        }.sortedByDescending { it.lastUsedOnTimestamp }
        val currentlySelectedPersona = state.value.personaListToDisplay.firstOrNull { it.selected }
            ?: updatedPersonas.firstOrNull { it.lastUsedOn != null }
        currentlySelectedPersona?.persona?.let {
            sendEvent(DAppSelectPersonaEvent.PersonaSelected(it))
        }
        return updatedPersonas.map { p ->
            p.copy(selected = p.persona.address == currentlySelectedPersona?.persona?.address)
        }
    }

    fun onSelectPersona(personaAddress: String) {
        val updatedPersonas = state.value.personaListToDisplay.map {
            it.copy(selected = it.persona.address == personaAddress)
        }.toPersistentList()
        _state.update { it.copy(personaListToDisplay = updatedPersonas, continueButtonEnabled = true) }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(DAppSelectPersonaEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }
}

sealed interface DAppSelectPersonaEvent : OneOffEvent {
    data class PersonaSelected(val persona: Network.Persona) : DAppSelectPersonaEvent
    data class CreatePersona(val firstPersonaCreated: Boolean) : DAppSelectPersonaEvent
}

data class SelectPersonaUiState(
    val isLoading: Boolean = true,
    val continueButtonEnabled: Boolean = false,
    val firstTimeLogin: Boolean = true,
    val personaListToDisplay: ImmutableList<PersonaUiModel> = persistentListOf()
) : UiState
