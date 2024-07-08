package com.babylon.wallet.android.presentation.dapp.authorized.selectpersona

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.babylon.wallet.android.utils.LAST_USED_DATE_FORMAT_SHORT_MONTH
import com.babylon.wallet.android.utils.toEpochMillis
import com.radixdlt.sargon.AuthorizedDapp
import com.radixdlt.sargon.IdentityAddress
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.preferences.PreferencesManager
import rdx.works.core.sargon.activePersonasOnCurrentNetwork
import rdx.works.profile.data.repository.DAppConnectionRepository
import rdx.works.profile.domain.GetProfileUseCase
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SelectPersonaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dAppConnectionRepository: DAppConnectionRepository,
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager
) : StateViewModel<SelectPersonaViewModel.State>(), OneOffEventHandler<SelectPersonaViewModel.Event> by OneOffEventHandlerImpl() {

    private val args = SelectPersonaArgs(savedStateHandle)

    override fun initialState(): State = State()

    init {
        observePersonas()
    }

    private fun observePersonas() {
        viewModelScope.launch {
            getProfileUseCase
                .flow
                .map { it.activePersonasOnCurrentNetwork }
                .collect { personas ->
                    val authorizedDApp = dAppConnectionRepository.getAuthorizedDApp(args.dappDefinitionAddress)
                    _state.update {
                        it.onProfileUpdated(
                            authorizedDApp = authorizedDApp,
                            profilePersonas = personas
                        )
                    }
                }
        }
    }

    fun onSelectPersona(personaAddress: IdentityAddress) {
        _state.update { it.onPersonaSelected(personaAddress) }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(Event.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    sealed interface Event : OneOffEvent {
        data class CreatePersona(val firstPersonaCreated: Boolean) : Event
    }

    data class State(
        val isLoading: Boolean = true,
        private val authorizedDApp: AuthorizedDapp? = null,
        val personas: ImmutableList<PersonaUiModel> = persistentListOf()
    ) : UiState {

        val selectedPersona: Persona? = personas.find { it.selected }?.persona

        val isFirstTimeLogin: Boolean
            get() = authorizedDApp == null

        val isContinueButtonEnabled: Boolean
            get() = selectedPersona != null

        fun onPersonaSelected(identityAddress: IdentityAddress): State = copy(
            personas = personas.map {
                it.copy(selected = identityAddress == it.persona.address)
            }.toImmutableList()
        )

        fun onProfileUpdated(
            authorizedDApp: AuthorizedDapp?,
            profilePersonas: List<Persona>,
        ): State {
            val authorizedPersonas = authorizedDApp?.referencesToAuthorizedPersonas

            val models = profilePersonas.map { persona ->
                val authorized = authorizedPersonas?.find { it.identityAddress == persona.address }

                persona.toUiModel().let { model ->
                    if (authorized != null) {
                        val localDateTime = authorized.lastLogin.toLocalDateTime()
                        model.copy(
                            lastUsedOn = localDateTime
                                ?.format(DateTimeFormatter.ofPattern(LAST_USED_DATE_FORMAT_SHORT_MONTH)),
                            lastUsedOnTimestamp = localDateTime?.toEpochMillis() ?: 0
                        )
                    } else {
                        model
                    }
                }
            }.sortedByDescending {
                it.lastUsedOnTimestamp
            }

            val modelsWithSelectedInfo = if (selectedPersona == null) {
                // When the view model is first created, or no personas exists, no prior selection exists
                // so we decide to pre-select the first one
                models.mapIndexed { index, personaUiModel ->
                    personaUiModel.copy(selected = index == 0)
                }
            } else {
                val previousPersonaAddresses = personas.map { it.persona.address }.toSet()
                val currentPersonaAddresses = profilePersonas.map { it.address }.toSet()

                val personaToSelect = currentPersonaAddresses.minus(previousPersonaAddresses).firstOrNull()

                // When a newly created persona is detected, we preselect that one
                if (personaToSelect != null) {
                    models.map {
                        it.copy(selected = it.persona.address == personaToSelect)
                    }
                } else {
                    models
                }
            }

            return copy(
                authorizedDApp = authorizedDApp,
                personas = modelsWithSelectedInfo.toImmutableList(),
                isLoading = false
            )
        }
    }
}
