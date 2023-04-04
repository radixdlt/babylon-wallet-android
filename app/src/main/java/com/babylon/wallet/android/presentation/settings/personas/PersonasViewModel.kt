package com.babylon.wallet.android.presentation.settings.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.PreferencesManager
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.personasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class PersonasViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel(), OneOffEventHandler<PersonasViewModel.PersonasEvent> by OneOffEventHandlerImpl() {

    private val _state = MutableStateFlow(PersonasUiState())
    internal val state: StateFlow<PersonasUiState> = _state

    init {
        viewModelScope.launch {
            getProfileUseCase.personasOnCurrentNetwork.collect { personas ->
                _state.update { it.copy(personas = personas.toPersistentList()) }
            }
        }
    }

    fun onCreatePersona() {
        viewModelScope.launch {
            sendEvent(PersonasEvent.CreatePersona(preferencesManager.firstPersonaCreated.first()))
        }
    }

    data class PersonasUiState(
        val personas: ImmutableList<Network.Persona> = persistentListOf()
    )

    sealed interface PersonasEvent : OneOffEvent {
        data class CreatePersona(val firstPersonaCreated: Boolean) : PersonasEvent
    }
}
