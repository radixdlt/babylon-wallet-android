package com.babylon.wallet.android.presentation.dapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.data.dapp.DAppAccountUiState
import com.babylon.wallet.android.data.dapp.PersonaEntityUiState
import com.babylon.wallet.android.data.profile.PersonaEntity
import com.babylon.wallet.android.domain.dapp.ConnectDAppUseCase
import com.babylon.wallet.android.domain.dapp.DAppState
import com.babylon.wallet.android.presentation.navigation.Destination
import com.babylon.wallet.android.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DAppViewModel @Inject constructor(
    private val connectDAppUseCase: ConnectDAppUseCase
) : ViewModel() {

    var state by mutableStateOf(DAppUiState())
        private set

//    var destination by mutableStateOf(DAppScreenState())
//        private set

    private lateinit var payloadLabels: List<String>
    private var payloadAddresses: Int = 0
    private var personas: MutableList<PersonaEntityUiState> = mutableListOf()
    private var accounts: MutableList<DAppAccountUiState> = mutableListOf()

    private val statesList = mutableListOf<DAppState>()
    private var currentStateIndex: Int = 0

    init {
        println("DApp connectDApp()")
        connectDApp()
    }

    fun onRequestConnectionContinueClick() {
        println("DApp showSelectPersonas")
        state = state.copy(
            dAppState = DAppState.SelectPersona(
                personas = personas,
                dismiss = false
            ),
            showProgress = false
        )
    }

    fun onSelectPersonaContinueClick() {
        state = state.copy(
            dAppState = DAppState.SelectAccount(
                accounts = accounts,
                dismiss = false
            ),
            showProgress = false
        )
    }

    fun onPersonaSelect(personaEntity: PersonaEntityUiState) {
        val updatedPersonas = personas.map { persona ->
            if (persona == personaEntity) {
                persona.copy(
                    personaEntity = personaEntity.personaEntity,
                    selected = !personaEntity.selected
                )
            }
            else personaEntity
        }
//        personas.find { personaEntity == it.personaEntity }?.copy(
//
//        )
//        val personas = personas.find { it.personaEntity == personaEntity }
        state = state.copy(
            dAppState = DAppState.SelectPersona(
                personas = updatedPersonas,
                dismiss = false
            ),
            showProgress = false
        )
    }

    fun onSelectAccountsContinueClick() {

    }

    private fun connectDApp(
        connectionId: String = "" // TODO to be provided from elsewhere
    ) {
        viewModelScope.launch {
            val payloadData = connectDAppUseCase(connectionId)
            payloadLabels = payloadData.payloadFields
            payloadAddresses = payloadData.addresses

//            connectDAppUseCase.getPersonas().forEach { persona ->
//                personas.add(
//                    PersonaEntityUiState(
//                        personaEntity = persona,
//                        selected = false
//                    )
//                )
//            }
//            connectDAppUseCase.getAccounts().forEach { account ->
//                accounts.add(
//                    DAppAccountUiState(
//                        account, false
//                    )
//                )
//            }

            if (payloadLabels.isNotEmpty()) {
                statesList.add(DAppState.ConnectionRequest(payloadLabels))
                statesList.add(DAppState.SelectPersona(
                    personas = personas,
                    dismiss = false
                ))
            }
            if (payloadAddresses > 0) {
                statesList.add(DAppState.SelectAccount(
                    accounts = accounts,
                    dismiss = false
                ))
            }

            if (statesList.isEmpty()) {
                //TODO Handle scenario when no screen should be shown ??
                throw Exception("No data to show")
            }

            println("DApp state showConnectionRequest")
            state = state.copy(
//                screen = Screen.DAppConnectionRequestDestination
                dAppState = DAppState.ConnectionRequest(
                    labels = payloadLabels
                ),
                showProgress = false
            )

//            state = state.copy(
//                loading = false,
//                dAppState = statesList[currentStateIndex]
//            )
        }
    }

//    fun onContinueClick() {
//        currentStateIndex++
//        state = if (statesList.size > currentStateIndex) {
//            state.copy(
//                dAppState = statesList[currentStateIndex]
//            )
//        } else {
//            // Last screen, finish
//            state.copy(
//                completeFlow = true
//            )
//        }
//    }
//
//    fun onBackClick() {
//        state = if (currentStateIndex == 0) {
//            state.copy(
//                completeFlow = true
//            )
//        } else {
//            currentStateIndex--
//            state.copy(
//                dAppState = statesList[currentStateIndex]
//            )
//        }
//    }

    fun onPersonaSelect(personaIndex: Int, personaSelected: Boolean) {
        val currentState = statesList[currentStateIndex]
        if (currentState is DAppState.SelectPersona) {
            val updatedPersonas = currentState.personas.mapIndexed { index, personaEntityUiState ->
                if (index == personaIndex) {
                    personaEntityUiState.copy(
                        personaEntity = personaEntityUiState.personaEntity,
                        selected = personaSelected
                    )
                }
                else personaEntityUiState
            }
//            state = state.copy(
//                dAppState = DAppState.SelectPersona(
//                    personas = updatedPersonas,
//                    dismiss = currentState.dismiss
//                )
//            )
        }
    }

    fun onAccountSelect(accountIndex: Int, accountSelected: Boolean) {
        val currentState = statesList[currentStateIndex]
        if (currentState is DAppState.SelectAccount) {
            val updatedAccounts = currentState.accounts.mapIndexed { index, account ->
                if (index == accountIndex) {
                    account.copy(
                        account = account.account,
                        selected = accountSelected
                    )
                }
                else account
            }
//            state = state.copy(
//                dAppState = DAppState.SelectAccount(
//                    accounts = updatedAccounts,
//                    dismiss = currentState.dismiss
//                )
//            )
        }
    }
}

//data class DAppUiState(
//    val loading: Boolean = true,
//    val dAppState: DAppState? = null,
//    val completeFlow: Boolean = false
//)

data class DAppScreenData(
    val screen: Screen? = null,
)
data class DAppUiState(
//    val screen: Screen? = null,
    val dAppState: DAppState? = null,
    val showProgress: Boolean = true,
//    val showConnectionRequest: DAppState.ConnectionRequest? = null,
//    val showSelectPersonas: DAppState.SelectPersona? = null,
//    val showSelectAccount: DAppState.SelectAccount? = null
//    val showSelectPersonas: Boolean = false
//    val screen: Screen? = null
//    val screen: (Screen?) -> Unit
)
