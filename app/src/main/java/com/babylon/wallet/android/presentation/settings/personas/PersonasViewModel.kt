package com.babylon.wallet.android.presentation.settings.personas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.Network
import rdx.works.profile.data.repository.PersonaRepository
import javax.inject.Inject

@HiltViewModel
class PersonasViewModel @Inject constructor(
    private val personaRepository: PersonaRepository
) : ViewModel() {

    internal var state by mutableStateOf(PersonasUiState())
        private set

    init {
        viewModelScope.launch {
            personaRepository.personas.collect { personas ->
                state = state.copy(personas = personas.toPersistentList())
            }
        }
    }

    data class PersonasUiState(
        val personas: ImmutableList<Network.Persona> = persistentListOf()
    )
}
