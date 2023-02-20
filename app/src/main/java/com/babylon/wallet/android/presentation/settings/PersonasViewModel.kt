package com.babylon.wallet.android.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import rdx.works.profile.data.model.pernetwork.OnNetwork
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
                state = state.copy(personas = personas)
            }
        }
    }

    data class PersonasUiState(
        val personas: List<OnNetwork.Persona> = emptyList()
    )
}
