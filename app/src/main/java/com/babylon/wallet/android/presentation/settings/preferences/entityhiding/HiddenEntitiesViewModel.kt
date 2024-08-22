package com.babylon.wallet.android.presentation.settings.preferences.entityhiding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.core.sargon.hiddenAccountsOnCurrentNetwork
import rdx.works.core.sargon.hiddenPersonasOnCurrentNetwork
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import javax.inject.Inject

@HiltViewModel
class HiddenEntitiesViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase
) : StateViewModel<State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.flow.map { it.hiddenAccountsOnCurrentNetwork },
                getProfileUseCase.flow.map { it.hiddenPersonasOnCurrentNetwork }
            ) { hiddenAccounts, hiddenPersonas ->
                hiddenAccounts to hiddenPersonas
            }.collect { hiddenAccountsToHiddenPersonas ->
                _state.update {
                    it.copy(
                        hiddenAccounts = hiddenAccountsToHiddenPersonas.first.toPersistentList(),
                        hiddenPersonas = hiddenAccountsToHiddenPersonas.second.toPersistentList()
                    )
                }
            }
        }
    }

    fun showUnhideAccountAlert(account: Account) {
        _state.update {
            it.copy(alertState = State.AlertState.ShowUnhideAccount(account))
        }
    }

    fun showUnhidePersonaAlert(persona: Persona) {
        _state.update {
            it.copy(alertState = State.AlertState.ShowUnhidePersona(persona))
        }
    }

    fun onDismissUnhideAlert() {
        _state.update {
            it.copy(alertState = State.AlertState.None)
        }
    }

    fun onUnhideSelectedEntity() {
        viewModelScope.launch {
            when (val alertState = state.value.alertState) {
                is State.AlertState.ShowUnhideAccount -> {
                    changeEntityVisibilityUseCase.changeAccountVisibility(alertState.account.address, hide = false)
                }

                is State.AlertState.ShowUnhidePersona -> {
                    changeEntityVisibilityUseCase.changePersonaVisibility(alertState.persona.address, hide = false)
                }

                else -> Unit
            }
            _state.update { it.copy(alertState = State.AlertState.None) }
        }
    }
}

sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class State(
    val hiddenAccounts: ImmutableList<Account>? = null,
    val hiddenPersonas: ImmutableList<Persona>? = null,
    val alertState: AlertState = AlertState.None
) : UiState {

    sealed interface AlertState {
        data class ShowUnhideAccount(val account: Account) : AlertState
        data class ShowUnhidePersona(val persona: Persona) : AlertState
        data object None : AlertState
    }
}
