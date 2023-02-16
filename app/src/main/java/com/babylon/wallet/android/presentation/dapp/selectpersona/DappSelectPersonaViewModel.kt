package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.domain.common.OneOffEvent
import com.babylon.wallet.android.domain.common.OneOffEventHandler
import com.babylon.wallet.android.domain.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.LAST_USED_PERSONA_DATE_FORMAT
import com.babylon.wallet.android.utils.fromISO8601String
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.OnNetwork
import rdx.works.profile.data.model.pernetwork.OnNetwork.ConnectedDapp.AuthorizedPersonaSimple
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.data.repository.PersonaRepository
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList")
class DappSelectPersonaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val personaRepository: PersonaRepository,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppSelectPersonaEvent> by OneOffEventHandlerImpl() {

    private val args = DappSelectPersonaArgs(savedStateHandle)

    private val authorizedRequest = incomingRequestRepository.getAuthorizedRequest(
        args.requestId
    )

    private val _state = MutableStateFlow(SelectPersonaUiState())
    val state = _state.asStateFlow()

    private var connectedDapp: OnNetwork.ConnectedDapp? = null

    init {
        viewModelScope.launch {
            connectedDapp = dAppConnectionRepository.getConnectedDapp(
                authorizedRequest.requestMetadata.dAppDefinitionAddress
            )
            val allAuthorizedPersonas = connectedDapp?.referencesToAuthorizedPersonas
            _state.update { state ->
                val personas = generatePersonasListForDisplay(
                    allAuthorizedPersonas,
                    state.personaListToDisplay
                ).toPersistentList()
                val selected = personas.any { it.selected }
                state.copy(
                    firstTimeLogin = connectedDapp == null,
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
            personaRepository.personas.collect { personas ->
                connectedDapp = dAppConnectionRepository.getConnectedDapp(
                    authorizedRequest.requestMetadata.dAppDefinitionAddress
                )
                val allAuthorizedPersonas = connectedDapp?.referencesToAuthorizedPersonas
                _state.update { state ->
                    state.copy(
                        personaListToDisplay = generatePersonasListForDisplay(
                            allAuthorizedPersonas,
                            personas.map { it.toUiModel() }
                        ).toPersistentList()
                    )
                }
            }
        }
    }

    private suspend fun generatePersonasListForDisplay(
        allAuthorizedPersonas: List<AuthorizedPersonaSimple>?,
        profilePersonas: List<PersonaUiModel>
    ): List<PersonaUiModel> {
        val defaultAuthorizedPersonaSimple = allAuthorizedPersonas?.firstOrNull()
        val authorizedPersona = defaultAuthorizedPersonaSimple?.let {
            personaRepository.getPersonaByAddress(it.identityAddress)
        }?.let {
            PersonaUiModel(
                it,
                selected = true,
                pinned = true,
                lastUsedOn = defaultAuthorizedPersonaSimple.lastUsedOn.fromISO8601String()
                    ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
            )
        }
        var updatedPersonas = profilePersonas.map { personaUiModel ->
            val matchingAuthorizedPersona = allAuthorizedPersonas?.firstOrNull {
                personaUiModel.persona.address == it.identityAddress
            }
            if (matchingAuthorizedPersona != null) {
                personaUiModel.copy(
                    lastUsedOn = matchingAuthorizedPersona.lastUsedOn.fromISO8601String()
                        ?.format(DateTimeFormatter.ofPattern(LAST_USED_PERSONA_DATE_FORMAT)),
                )
            } else {
                personaUiModel
            }
        }
        val selectedPersona = state.value.personaListToDisplay.firstOrNull { it.selected }
        selectedPersona?.persona?.let {
            sendEvent(DAppSelectPersonaEvent.PersonaSelected(it))
        }
        updatedPersonas = if (selectedPersona != null && selectedPersona != authorizedPersona) {
            updatedPersonas.map { p -> p.copy(selected = p.persona.address == selectedPersona.persona.address) }
        } else {
            updatedPersonas.filter { it.persona.address != authorizedPersona?.persona?.address }
        }
        return authorizedPersona?.let { listOf(authorizedPersona) }.orEmpty() + updatedPersonas
    }

    fun onSelectPersona(personaAddress: String) {
        val updatedPersonas = state.value.personaListToDisplay.map {
            it.copy(selected = it.persona.address == personaAddress)
        }.toPersistentList()
        _state.update { it.copy(personaListToDisplay = updatedPersonas, continueButtonEnabled = true) }
    }
}

sealed interface DAppSelectPersonaEvent : OneOffEvent {
    data class PersonaSelected(val persona: OnNetwork.Persona) : DAppSelectPersonaEvent
}

data class SelectPersonaUiState(
    val isLoading: Boolean = true,
    val continueButtonEnabled: Boolean = false,
    val firstTimeLogin: Boolean = true,
    val personaListToDisplay: ImmutableList<PersonaUiModel> = persistentListOf()
)
