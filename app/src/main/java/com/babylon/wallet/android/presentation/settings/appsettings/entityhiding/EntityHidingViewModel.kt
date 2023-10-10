package com.babylon.wallet.android.presentation.settings.appsettings.entityhiding

import androidx.lifecycle.viewModelScope
import com.babylon.wallet.android.presentation.common.OneOffEvent
import com.babylon.wallet.android.presentation.common.OneOffEventHandler
import com.babylon.wallet.android.presentation.common.OneOffEventHandlerImpl
import com.babylon.wallet.android.presentation.common.StateViewModel
import com.babylon.wallet.android.presentation.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rdx.works.profile.domain.ChangeEntityVisibilityUseCase
import rdx.works.profile.domain.GetProfileUseCase
import rdx.works.profile.domain.hiddenAccountsOnCurrentNetwork
import rdx.works.profile.domain.hiddenPersonasOnCurrentNetwork
import javax.inject.Inject

@HiltViewModel
class EntityHidingViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val changeEntityVisibilityUseCase: ChangeEntityVisibilityUseCase
) : StateViewModel<State>(), OneOffEventHandler<Event> by OneOffEventHandlerImpl() {

    override fun initialState(): State = State()

    init {
        viewModelScope.launch {
            combine(
                getProfileUseCase.hiddenAccountsOnCurrentNetwork,
                getProfileUseCase.hiddenPersonasOnCurrentNetwork
            ) { hiddenAccounts, hiddenPersonas ->
                hiddenAccounts.size to hiddenPersonas.size
            }.collect { hiddenAccountsToHiddenPersonas ->
                _state.update {
                    it.copy(hiddenAccounts = hiddenAccountsToHiddenPersonas.first, hiddenPersonas = hiddenAccountsToHiddenPersonas.second)
                }
            }
        }
    }

    fun onUnhideAllAccounts() {
        viewModelScope.launch {
            changeEntityVisibilityUseCase.unhideAll()
        }
    }
}

sealed interface Event : OneOffEvent {
    data object Close : Event
}

data class State(
    val hiddenAccounts: Int = 0,
    val hiddenPersonas: Int = 0
) : UiState
