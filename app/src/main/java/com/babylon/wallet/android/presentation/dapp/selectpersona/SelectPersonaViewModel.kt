package com.babylon.wallet.android.presentation.dapp.selectpersona

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.IncomingRequestRepository
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.utils.LAST_USED_PERSONA_DATE_FORMAT
import com.babylon.wallet.android.utils.fromISO8601String
import com.babylon.wallet.android.utils.toEpochMillis
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
class SelectPersonaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val personaRepository: PersonaRepository,
    incomingRequestRepository: IncomingRequestRepository
) : ViewModel(), OneOffEventHandler<DAppSelectPersonaEvent> by OneOffEventHandlerImpl() {

    private val args = SelectPersonaArgs(savedStateHandle)

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
