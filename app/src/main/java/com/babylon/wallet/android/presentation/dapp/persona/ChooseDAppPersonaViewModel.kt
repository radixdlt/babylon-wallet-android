// TODO this might be reverted later on
// package com.babylon.wallet.android.presentation.dapp.persona
//
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.setValue
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import com.babylon.wallet.android.data.dapp.PersonaEntityUiState
// import com.babylon.wallet.android.domain.dapp.GetDAppPersonasUseCase
// import com.babylon.wallet.android.presentation.navigation.Screen
// import dagger.hilt.android.lifecycle.HiltViewModel
// import kotlinx.coroutines.delay
// import kotlinx.coroutines.launch
// import javax.inject.Inject
//
// @HiltViewModel
// class ChooseDAppPersonaViewModel @Inject constructor(
//    private val getDAppPersonasUseCase: GetDAppPersonasUseCase
// ) : ViewModel() {
//
//    var personasState by mutableStateOf(ChoosePersonaUiState())
//        private set
//
//    init {
//        viewModelScope.launch {
//            val personas = getDAppPersonasUseCase.getDAppPersonas()
//            delay(1000)
//            personasState = personasState.copy(
//                personas = personas,
//                initialPage = false,
//                destination = Screen.DAppChooseAccountDestination
//            )
//        }
//    }
//
//    fun onPersonaSelect(persona: PersonaEntityUiState) {
//        val updatedPersonas = personasState.personas?.map { personaEntityUiState ->
//            if (personaEntityUiState == persona) {
//                personaEntityUiState.copy(
//                    personaEntity = personaEntityUiState.personaEntity,
//                    selected = !personaEntityUiState.selected
//                )
//            }
//            else personaEntityUiState
//        }
//        personasState = personasState.copy(
//            personas = updatedPersonas
//        )
//    }
// }
//
// data class ChoosePersonaUiState(
//    val personas: List<PersonaEntityUiState>? = null,
//    val initialPage: Boolean = true,
//    val destination: Screen? = null
// )
